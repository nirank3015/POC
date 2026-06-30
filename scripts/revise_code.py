import json, os, re, urllib.request, sys

design_doc     = os.environ.get("DESIGN_DOC", "")
instructions   = os.environ.get("INSTRUCTIONS", "")
source_context = os.environ.get("SOURCE_CONTEXT", "")
feedback       = os.environ.get("FEEDBACK", "")
ticket_id      = os.environ.get("TICKET_ID", "")
gh_token       = os.environ.get("GH_TOKEN", "")
module_paths_raw = os.environ.get("MODULE_PATHS", "").strip()
known_modules = [m.strip().rstrip("/") for m in module_paths_raw.split(",") if m.strip()]

if known_modules:
    modules_list = "\n".join("- " + m for m in known_modules)
    module_instruction = (
        "The available modules are:\n" + modules_list + "\n"
        "Every file path MUST start with the correct module name (e.g. product-crud/src/main/java/...)."
    )
    example_module = known_modules[0] + "/"
else:
    module_instruction = "Use paths relative to the repository root."
    example_module = ""

prompt = "\n\n".join([
    "You are a senior Java Spring Boot developer. You wrote the following code and a code reviewer has requested changes.",
    "## Project Conventions\n" + instructions,
    "## Design Document (approved)\n" + design_doc,
    "## Current Code (files changed on this branch)\n" + source_context,
    "## Reviewer Feedback\n" + feedback,
    "## Instructions\nRevise only the files that need to change to address the feedback. Output ONLY the files that need updating using EXACTLY this format:\n\n### FILE: " + example_module + "src/main/java/com/example/[path]/[ClassName].java\n```java\n[complete revised file content]\n```\n\nDo not output files that don't need changes. Write complete, compilable code.\n\n" + module_instruction,
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

text = data["choices"][0]["message"]["content"]

file_pattern = re.compile(
    r'### FILE: ([\w/.\-]+)\n```(?:java|sql|xml|yaml|yml|properties)?\n(.*?)```',
    re.DOTALL
)

files = file_pattern.findall(text)
for filepath, content in files:
    filepath = filepath.strip()
    if known_modules:
        has_module_prefix = any(filepath.startswith(m + "/") for m in known_modules)
        if not has_module_prefix:
            if len(known_modules) == 1:
                filepath = known_modules[0] + "/" + filepath
                print("WARNING: module prefix missing, defaulted to: " + known_modules[0])
            else:
                print("ERROR: cannot determine module for '" + filepath + "' — skipping.")
                continue
    dir_part = os.path.dirname(filepath)
    if dir_part:
        os.makedirs(dir_part, exist_ok=True)
    with open(filepath, "w") as f:
        f.write(content.strip())
    print("Revised: " + filepath)

print("\nTotal files revised: " + str(len(files)))
