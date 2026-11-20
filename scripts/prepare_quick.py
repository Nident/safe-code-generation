#!/usr/bin/env python3
"""Create 13 generation tasks: 2 per category except one verified CWE-94 candidate."""
import collections
import json
import shutil
from pathlib import Path
from prepare_data import BASE_WRAPPER,MCP_WRAPPER,method_fragment,sha,write_json
from quick_specs import EXTRA

ROOT=Path(__file__).resolve().parents[1]

def main():
 rows={r['source']['index']:r for r in map(json.loads,(ROOT/'generated-code/data/clear_samples.jsonl').read_text().splitlines())}
 originals=json.loads((ROOT/'generated-code/data/pilot_v1.json').read_text())
 destination=ROOT/'generated-code/quick-data';prompt_root=ROOT/'prompts/quick-v1'
 tasks=[]
 for cwe in sorted({t['cwe'] for t in originals},key=lambda c:int(c[4:])):
  source=next(t for t in originals if t['cwe']==cwe)
  selections=[(source['source_index'],source)] + [(i,None) for i,s in EXTRA.items() if s['cwe']==cwe]
  for index,original in selections:
   row=rows[index];assert not row['prepared']['leakage']['known_overlap']
   taskid=f'quick-{len(tasks)+1:02d}';pf=prompt_root/taskid;pf.mkdir(parents=True,exist_ok=True)
   if original:
    task=dict(original);common=(ROOT/original['prompt_dir']/'task.md').read_text().replace(original['task_id'],taskid)
   else:
    spec=EXTRA[index];common=f"Task: {taskid}\n\nSignature:\n{spec['signature']}\n\nFunctional requirements:\n{spec['description']}\n\nAvailable context and dependencies:\n{spec['context']}\n"
    ref=destination/'references'/row['sample_id'];ref.mkdir(parents=True,exist_ok=True)
    targets={}
    for field in ['func_before','func_after']:
     code,first,last=method_fragment(row['source'][field],spec['signature']);path=ref/(field+'.java');path.write_text(code)
     targets[field]={'path':str(path.relative_to(ROOT)),'source_lines':[first,last]}
    task=dict(sample_id=row['sample_id'],cwe=cwe,source_index=index,target=targets,reference_path_base='project',
      mechanism_review='static_candidate_not_dynamically_validated',evidence=spec['evidence'],next_adaptation_step=spec['adaptation'],preferred_attack_candidates=spec['preferred'],
      generation_enabled=True,dynamic_scoring_eligible=False,eligibility_reason='No adapter/before-after control validation',leakage=row['prepared']['leakage'])
   task.update(task_id=taskid,prompt_dir=str(pf.relative_to(ROOT)))
   (pf/'task.md').write_text(common)
   for variant,wrapper in [('llm',BASE_WRAPPER),('llm_mcp',MCP_WRAPPER)]:
    (pf/(variant+'.txt')).write_text(wrapper+'\n'+common)
   tasks.append(task)
 write_json(destination/'tasks.json',tasks)
 write_json(destination/'manifest.json',dict(counts=dict(collections.Counter(t['cwe'] for t in tasks)),
  limitation='No second defensible CWE-94 source example: indices 178 and 239 have unconfirmed labels. 13 tasks, not 14. All tasks are generation-only; task from index 237 has unconfirmed URI validation context.',
  files={str(p.relative_to(ROOT)):sha(p) for base in [prompt_root,destination/'references'] for p in sorted(base.rglob('*')) if p.is_file()},
  tasks_sha256=sha(destination/'tasks.json')))
 print('Prepared',len(tasks),'tasks:',dict(collections.Counter(t['cwe'] for t in tasks)))
if __name__=='__main__':main()
