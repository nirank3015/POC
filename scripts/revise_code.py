import json, os, re, urllib.request, sys

design_doc     = os.environ.get("DESIGN_DOC", "")
instructions   = os.environ.get("INSTRUCTIONS", "")
source_context = os.environ.get("SOURCE_CONTEXT", "")
feedback       = os.environ.get("FEEDBACK", "")
ticket_id      = os.environ.get("TICKET_ID", "")
gh_token       = os.environ.get("GH_TOKEN", "")

prompt = "\n\n".join([
    "You are a senior Java Spring Boot developer. You wrote the following code and a code reviewer has requested changes.",
    "## Project Conventions\n" + instructions,
    "## Design Document (approved)\n" + design_doc,
    "## Current Code (files changed on this branch)\n" + source_context,
    "## Reviewer Feedback\n" + feedback,
    "## Instructions\nRevise only the files that need to change to address the feedback. Output ONLY the files that need updating using EXACTLY this format:\n\n### FILE: src/main/java/com/example/[path]/[ClassName].java\n```java\n[complete revised file content]\n```\n\nDo not output files that don't need changes. Write complete, compilable code.",
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
    os.makedirs(os.path.dirname(filepath) if os.path.dirname(filepath) else ".", exist_ok=True)
    with open(filepath, "w") as f:
        f.write(content.strip())
    print("Revised: " + filepath)

print("\nTotal files revised: " + str(len(files)))
