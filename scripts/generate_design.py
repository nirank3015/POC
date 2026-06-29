import json, os, urllib.request, sys

requirement  = os.environ.get("REQUIREMENT", "")
instructions = os.environ.get("INSTRUCTIONS", "")
ticket_id    = os.environ.get("TICKET_ID", "")
gh_token     = os.environ.get("GH_TOKEN", "")

sections = [
    "## 1. Overview\nBrief summary of the feature/fix and its purpose.",
    "## 2. Scope\nWhat is in scope and explicitly out of scope.",
    "## 3. API Design\n- Endpoint(s): method, path, request body, response body, status codes\n- Request/Response examples in JSON",
    "## 4. Data Model Changes\n- New or modified JPA entities (fields, types, constraints)\n- Database migration notes (new tables, columns, indexes)",
    "## 5. Service Layer Design\n- New or modified service classes and methods\n- Business logic and validation rules\n- Transaction boundaries",
    "## 6. Repository Layer\n- New Spring Data JPA repository methods or Specifications needed\n- Any custom JPQL/native queries",
    "## 7. Security & Validation\n- Input validation rules (@Valid annotations, constraints)\n- Authorization/role requirements",
    "## 8. Error Handling\n- Expected exceptions and how they map to HTTP responses\n- Custom error codes if applicable",
    "## 9. Testing Strategy\n- Unit test cases (service layer)\n- Integration test cases (controller layer)\n- Edge cases to cover",
    "## 10. Open Questions\nAnything that needs human clarification before implementation.",
]

prompt = "\n\n".join([
    "You are a senior Java Spring Boot architect. Generate a thorough technical design document for the following requirement.",
    "## Project Conventions\n" + instructions,
    "## Requirement\n" + requirement,
    "## Output Format\nProduce a Markdown document with these exact sections:\n\n# Technical Design: " + ticket_id + "\n\n" + "\n\n".join(sections),
    "Be specific and concrete. Use the project conventions above to align with existing patterns.",
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
with open("/tmp/design_doc.md", "w") as f:
    f.write(text)
print("Design doc generated successfully.")
