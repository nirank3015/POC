import json, os, re, sys, urllib.error, urllib.request

design_doc     = os.environ.get("DESIGN_DOC", "")
instructions   = os.environ.get("INSTRUCTIONS", "")
source_context = os.environ.get("SOURCE_CONTEXT", "")
pom_content    = os.environ.get("POM_CONTENT", "")
ticket_id      = os.environ.get("TICKET_ID", "")
gh_token       = os.environ.get("GH_TOKEN", "")
module_paths_raw = os.environ.get("MODULE_PATHS", "").strip()
# List of known module prefixes, e.g. ["product-crud", "order-service"]
known_modules = [m.strip().rstrip("/") for m in module_paths_raw.split(",") if m.strip()]

output_rules = "\n".join([
    "Rules:",
    "- Match the exact package structure from the existing codebase",
    "- Follow every naming convention from copilot-instructions.md",
    "- Use existing base classes, interfaces, and utilities where applicable",
    "- Include all imports",
    "- Write complete, compilable code — no TODOs, no placeholders",
    "- Include unit tests for service layer",
    "- Include integration tests for controller layer using @WebMvcTest",
])

if known_modules:
    modules_list = "\n".join("- " + m for m in known_modules)
    module_instruction = (
        "This is a multi-module Maven/Gradle project. The available modules are:\n"
        + modules_list + "\n\n"
        + "IMPORTANT: Every file path MUST start with the correct module name. "
        + "Choose the module that logically owns each file based on its package and responsibility. "
        + "Example for module 'product-crud': ### FILE: product-crud/src/main/java/..."
    )
    example_module = known_modules[0]
else:
    module_instruction = "Use paths relative to the repository root."
    example_module = ""

prefix = (example_module + "/") if example_module else ""
output_format = "\n\n".join([
    "Generate ALL required Java files. For each file output EXACTLY this format:",
    "### FILE: " + prefix + "src/main/java/com/example/[path]/[ClassName].java\n```java\n[complete file content]\n```",
    "### FILE: " + prefix + "src/test/java/com/example/[path]/[ClassName]Test.java\n```java\n[complete test file content]\n```",
    "### FILE: " + prefix + "src/main/resources/db/migration/V[timestamp]__[description].sql\n```sql\n[migration content]\n```",
    output_rules,
    module_instruction,
])

prompt = "\n\n".join([
    "You are a senior Java Spring Boot developer. Based on the approved technical design document, generate production-ready Spring Boot code.",
    "## Project Conventions\n" + instructions,
    "## Existing pom.xml (for dependency awareness — do NOT add new dependencies unless essential)\n" + pom_content,
    "## Existing Codebase Context (follow these patterns exactly)\n" + source_context,
    "## Approved Technical Design Document\n" + design_doc,
    "## Output Instructions\n" + output_format,
])

payload = {
    "model": "gpt-4o",
    "max_tokens": 8000,
    "messages": [{"role": "user", "content": prompt}]
}

req = urllib.request.Request(
    "https://models.inference.ai.azure.com/chat/completions",
    data=json.dumps(payload).encode(),
    headers={"Authorization": "Bearer " + gh_token, "Content-Type": "application/json"}
)

try:
    with urllib.request.urlopen(req) as resp:
        data = json.load(resp)
except urllib.error.HTTPError as e:
    error_body = e.read().decode("utf-8", errors="replace")
    print(f"ERROR: GitHub Models request failed with HTTP {e.code} {e.reason}", file=sys.stderr)
    if error_body:
        print(error_body, file=sys.stderr)
    raise

if "choices" not in data:
    print("ERROR: " + json.dumps(data), file=sys.stderr)
    sys.exit(1)

text = data["choices"][0]["message"]["content"]

file_pattern = re.compile(
    r'### FILE: ([\w/.\-]+)\n```(?:java|sql|xml|yaml|yml|properties)?\n(.*?)```',
    re.DOTALL
)

files = file_pattern.findall(text)
if not files:
    print("WARNING: No files parsed from response. Dumping raw response.")
    os.makedirs("generated", exist_ok=True)
    with open("generated/" + ticket_id + "-raw.md", "w") as f:
        f.write(text)
    sys.exit(0)

written = []
for filepath, content in files:
    filepath = filepath.strip()
    if known_modules:
        has_module_prefix = any(filepath.startswith(m + "/") for m in known_modules)
        if not has_module_prefix:
            # AI omitted the module prefix — fall back: if only one module exists use it,
            # otherwise warn and skip to avoid writing to the wrong place
            if len(known_modules) == 1:
                filepath = known_modules[0] + "/" + filepath
                print("WARNING: module prefix missing, defaulted to: " + known_modules[0])
            else:
                print("ERROR: cannot determine module for path '" + filepath
                      + "' — skipping. AI must prefix with one of: " + ", ".join(known_modules))
                continue
    dir_part = os.path.dirname(filepath)
    if dir_part:
        os.makedirs(dir_part, exist_ok=True)
    with open(filepath, "w") as f:
        f.write(content.strip())
    print("Written: " + filepath)
    written.append(filepath)

with open("/tmp/generated_files.txt", "w") as f:
    f.write("\n".join(written))

print("\nTotal files generated: " + str(len(files)))
