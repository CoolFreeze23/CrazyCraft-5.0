import json

inv = json.load(open(r"C:\Homework\Projects\CrazyCraft5\docs\mod_inventory.json", encoding="utf-8"))
for i in inv:
    env = i.get("environment", "")
    env = f" [{env}]" if env and env != "*" else ""
    print(f"{i['modid']:35} {str(i['version'])[:25]:25} {i['loader'][:8]:8}{env}  {i['file']}")
