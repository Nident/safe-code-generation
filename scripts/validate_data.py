#!/usr/bin/env python3
"""Offline validation of prepared reference data and blind prompts."""
import argparse
import collections
import json
import re
from pathlib import Path
from prepare_data import EXPECTED, GROUPS, norm, sha

def validate(root):
 data=root/'generated-code/data';manifest=json.loads((data/'manifest.json').read_text())
 for rel,expected in manifest['files'].items():
  path=root/rel
  assert path.is_file(),f'Missing: {rel}'
  assert sha(path)==expected,f'Hash mismatch: {rel}'
 rows=[json.loads(l) for l in (data/'clear_samples.jsonl').read_text().splitlines()]
 assert len(rows)==127 and len({r['sample_id'] for r in rows})==127
 assert collections.Counter(r['source']['cwe_id'] for r in rows)==EXPECTED
 for r in rows:
  src=r['source'];folder=data/'samples'/src['cwe_id']/r['sample_id']
  for file,key in [('func_before.java','func_before'),('func_after.java','func_after'),('func.diff','func_diff')]:
   if key in src:assert (folder/file).read_bytes()==src[key].encode(),str(folder/file)
   else:assert r['prepared']['diff_origin']=='derived_unified_diff_source_field_absent'
  assert r['prepared']['compatibility']=='requires_adaptation'
 mappings=json.loads((data/'sec_code_bench/mapping.json').read_text());assert len(mappings)==37
 assert {m['task_id'] for m in mappings}=={x for v in GROUPS.values() for x in v}
 for m in mappings:
  assert m['security_tests'] and m['functional_tests']
  assert (data/m['template']/'pom.xml').is_file()
  for rel in m['security_tests']+m['functional_tests']:assert (data/rel).is_file(),rel
  for ev in m['evidence']:assert sha(data/ev['path'])==ev['sha256']
 # Copies must be byte-identical to the source hashes retained at preparation time.
 for rel,h in json.loads((data/'sec_code_bench/source_file_hashes.json').read_text()).items():
  assert sha(data/'sec_code_bench/templates'/rel)==h,rel
 pilots=json.loads((data/'pilot_v1.json').read_text());assert len(pilots)==7
 assert {p['cwe'] for p in pilots}==set(GROUPS)
 byid={r['sample_id']:r for r in rows}
 for task in pilots:
  assert not task['dynamic_scoring_eligible']
  src=byid[task['sample_id']]['source'];pf=root/task['prompt_dir']
  common=(pf/'task.md').read_text()
  for variant in ['llm','llm_mcp']:
   text=(pf/f'{variant}.txt').read_text()
   assert text.endswith(common)
   assert not re.search(r'(?:CWE-\d+|CVE-\d{4}-\d+|GHSA-[\w-]+|func_before|func_after)',text),pf
   assert src['commit_hash'] not in text and src['cve_id'] not in text
   for ref in ['func_before','func_after']:
    assert norm(src[ref]) not in norm(text),'Full reference leaked'
    frag=(data/task['target'][ref]['path']).read_text()
    assert norm(frag) not in norm(text),'Target body leaked'
    assert frag in src[ref],'Extraction differs from original'
  assert not task['leakage']['known_overlap'],f'Pilot overlap: {task["task_id"]}'
 assert manifest['generator_sha256']==sha(Path(__file__).with_name('prepare_data.py'))
 assert manifest['pilot_specs_sha256']==sha(Path(__file__).with_name('pilot_specs.py'))
 print('OK: 127 exact CLEAR records, 37 template trees, 7 blind task pairs; inventory and overlap checks passed.')

def main():
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('--root',type=Path,default=Path(__file__).resolve().parents[1]);args=p.parse_args();validate(args.root.resolve())
if __name__=='__main__':main()
