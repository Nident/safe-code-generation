#!/usr/bin/env python3
"""Report generation status and lexical similarity. Does not execute or prove code security."""
import argparse
import csv
import difflib
import json
import re
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
def similarity(a,b):
 # Whitespace-only normalization: still text similarity, not semantic equivalence.
 return round(difflib.SequenceMatcher(None,re.sub(r'\s+','',a),re.sub(r'\s+','',b),autojunk=False).ratio(),4)
def main():
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('--run-id',default='v1');p.add_argument('--task-manifest',type=Path,default=Path('generated-code/data/pilot_v1.json'));a=p.parse_args()
 tasks={t['sample_id']:t for t in json.loads((ROOT/a.task_manifest).read_text())}
 folder=ROOT/'generated-code'/a.run_id;records=[]
 for file in sorted(folder.glob('*/*/run.json')):
  run=json.loads(file.read_text());task=tasks.get(run['sample_id']);finalpath=file.with_name('final.json')
  final=json.loads(finalpath.read_text()) if finalpath.exists() else {};code=final.get('code','')
  record=dict(sample_id=run['sample_id'],variant=run['variant'],generation_status=run['status'],functional_tests='NOT_RUN',security_tests='NOT_RUN',similarity_before='',similarity_after='')
  if task and code:
   base=ROOT if task.get('reference_path_base')=='project' else ROOT/'generated-code/data'
   for key in ['before','after']:record['similarity_'+key]=similarity(code,(base/task['target']['func_'+key]['path']).read_text())
  records.append(record)
 if not records:raise SystemExit('No run.json files found')
 target=folder/'generation_report.csv'
 with target.open('w',newline='') as f:
  writer=csv.DictWriter(f,fieldnames=list(records[0]));writer.writeheader();writer.writerows(records)
 print(target)
 print('Text similarity only. Functional/security tests NOT RUN.')
if __name__=='__main__':main()
