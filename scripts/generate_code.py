import json, os, re, urllib.request, sys

design_doc     = os.environ.get("DESIGN_DOC", "")
instructions   = os.environ.get("INSTRUCTIONS", "")
source_context = os.environ.get("SOURCE_CONTEXT", "")
pom_content    = os.environ.get("POM_CONTENT", "")
ticket_id      = os.environ.get("TICKET_ID", "")
gh_token       = os.environ.get("GH_TOKEN", "")

output_rules = "\n".join([
    "Rules:",
    "- Match the exact package structure from the existing codebase",
    "- Follow every naming convention from copilot-instructions.md",
    "- Use existing base classes, interfaces, and utilities where applicable",
    "- Include all imports",
    "- Write complete, compilable code — no TODOs, no placeholders",
    "- Include unit tests for service layer",
    "- Include integration tests for controller layer using @WebMvcTest",
    "- If DB migration is needed, output: ### FILE: src/main/resources/db/migration/V[timestamp]__[description].sql",
])

output_format = "\n\n".join([
    "Generate ALL required Java files. For each file output EXACTLY this format:",
    "### FILE: src/main/java/com/example/[path]/[ClassName].java\n```java\n[complete file content]\n```",
    "### FILE: src/test/java/com/example/[path]/[ClassName]Test.java\n```java\n[complete test file content]\n```",
    output_rules,
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

with urllib.request.urlopen(req) as resp:
    data = json.load(resp)

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

for filepath, content in files:
    filepath = filepath.strip()
    os.makedirs(os.path.dirname(filepath) if os.path.dirname(filepath) else ".", exist_ok=True)
    with open(filepath, "w") as f:
        f.write(content.strip())
    print("Written: " + filepath)

with open("/tmp/generated_files.txt", "w") as f:
    f.write("\n".join(fp.strip() for fp, _ in files))

print("\nTotal files generated: " + str(len(files)))
