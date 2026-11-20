#!/usr/bin/env python3
"""Run isolated Codex generation, or print the exact launch plan with --dry-run."""
import argparse
import datetime
import hashlib
import json
import re
import shutil
import subprocess
import tempfile
import urllib.request
import uuid
from pathlib import Path
from prepare_data import sha

REQUIRED_TOOLS={'vckg_search_root_causes','vckg_search_fix_intents'}

def audit_events(path,variant):
 """Require actual completed MCP items, never accept prose claiming a tool call."""
 completed=[];errors=[];parse_errors=[];all_calls=[]
 for n,line in enumerate(path.read_text().splitlines(),1):
  try: event=json.loads(line)
  except json.JSONDecodeError:
   parse_errors.append(n);continue
  if event.get('type') in {'error','turn.failed'}:errors.append(event)
  item=event.get('item',{})
  if item.get('type') in {'command_execution','web_search','collab_tool_call'}:
   errors.append('Forbidden non-VCKG tool use: '+str(item.get('type')))
  if item.get('type')!='mcp_tool_call':continue
  if event.get('type')=='item.completed':
   all_calls.append(item)
   if item.get('server')!='vckg':errors.append('Unexpected MCP server')
   if item.get('status')!='completed' or item.get('error') or item.get('result') is None:
    errors.append(item)
   elif not isinstance(item.get('result'),dict) or item['result'].get('isError'):
    errors.append(item)
   else:completed.append(item)
 tools={x.get('tool') for x in completed if x.get('server')=='vckg'}
 unknown_schema=variant=='llm_mcp' and not all_calls
 valid=(not errors and not parse_errors and (REQUIRED_TOOLS<=tools if variant=='llm_mcp' else not all_calls))
 calls=[x for x in completed if x.get('tool') in REQUIRED_TOOLS]
 analysis_ids=set()
 for call in calls:
  args=call.get('arguments',{})
  if isinstance(args,str):
   try:args=json.loads(args)
   except json.JSONDecodeError:args={}
  analysis_ids.add(args.get('analysis_id'))
  if re.search(r'(?:CWE|CVE|GHSA)-',str(args.get('query','')),re.I):valid=False;errors.append('Identifier in retrieval query')
 if variant=='llm_mcp' and (len(analysis_ids)!=1 or None in analysis_ids or '' in analysis_ids):
  valid=False;errors.append('Missing or inconsistent analysis_id')
 return dict(valid=valid,completed_tools=sorted(t for t in tools if t),calls=all_calls,errors=errors,
             unparseable_event_lines=parse_errors,unknown_or_missing_mcp_event_schema=unknown_schema,
             no_relevant_match_count=sum('no_relevant_match' in json.dumps(x.get('result')) for x in completed))

def main():
 p=argparse.ArgumentParser(description=__doc__)
 p.add_argument('--root',type=Path,default=Path(__file__).resolve().parents[1]);p.add_argument('--model',required=True)
 p.add_argument('--reasoning-effort',choices=['low','medium','high','xhigh'],default='medium')
 p.add_argument('--variant',choices=['llm','llm_mcp','both'],default='both')
 p.add_argument('--task-manifest',type=Path,default=Path('generated-code/data/pilot_v1.json'),help='Task list path relative to project root or absolute')
 p.add_argument('--samples',default='all',help='Comma-separated task IDs, sample IDs or source indices; default all')
 p.add_argument('--image',default='clear-codex:v1');p.add_argument('--auth-volume',default='clear-codex-auth')
 p.add_argument('--mcp-url',default='http://host.docker.internal:4250/mcp')
 p.add_argument('--health-url',default='http://127.0.0.1:4250/health')
 p.add_argument('--vckg-dir',type=Path,help='External runtime directory, required for MCP runs, including dry runs')
 p.add_argument('--run-id',default='v1',help='Fresh output group; v1 matches the documented paths')
 p.add_argument('--timeout',type=int,default=900);p.add_argument('--dry-run',action='store_true')
 a=p.parse_args();root=a.root.resolve()
 if not re.fullmatch(r'[\w.-]+',a.run_id) or a.run_id in ('.','..'):p.error('Invalid run-id')
 task_manifest=root/a.task_manifest
 tasks=json.loads(task_manifest.read_text())
 if a.samples!='all':
  requested=set(a.samples.split(','));selected=[]
  for t in tasks:
   if requested&{t['task_id'],t['sample_id'],str(t['source_index'])}:selected.append(t)
  resolved={key for t in selected for key in (t['task_id'],t['sample_id'],str(t['source_index']))}
  if requested-resolved:p.error('Unknown sample selector: '+str(requested-resolved))
  tasks=selected
 variants=['llm','llm_mcp'] if a.variant=='both' else [a.variant]
 if 'llm_mcp' in variants and not a.vckg_dir:p.error('--vckg-dir required for runtime provenance')
 provenance={}
 if a.vckg_dir:
  for label,file in [('graph','knowledge_graph/project_lvl_final_knowledge_graph.json'),('config','vckg_tool_config.yaml')]:
   provenance[label+'_sha256']=sha(a.vckg_dir/file)
  provenance['runtime_files']={str(f.relative_to(a.vckg_dir)):sha(f) for f in sorted((a.vckg_dir/'vckg_mcp').glob('*.py'))}
  manifest=json.loads((root/'generated-code/data/manifest.json').read_text())
  for label,source in [('graph','graph'),('config','vckg_config')]:
   if provenance[label+'_sha256']!=manifest['sources'][source]['sha256']:
    p.error(f'{label} differs from prepared experiment; rebuild data or restore pinned runtime')
 schema=root/'prompts/v1/output.schema.json'
 # Fail before any paid invocation if any requested output already exists.
 for task in tasks:
  for variant in variants:
   dest=root/'generated-code'/a.run_id/variant/task['sample_id']
   if dest.exists() and not a.dry_run:p.error(f'Output exists: {dest}; use a new --run-id, never overwrite a trial')
 health=None
 version=None;image_id=None
 if not a.dry_run:
  for key,command in [('version',['docker','run','--rm',a.image,'--version']),('image_id',['docker','image','inspect',a.image,'--format','{{.Id}}'])]:
   check=subprocess.run(command,capture_output=True,text=True,timeout=30,check=True)
   if key=='version':version=check.stdout.strip()
   else:image_id=check.stdout.strip()
 if not a.dry_run and 'llm_mcp' in variants:
  with urllib.request.urlopen(a.health_url,timeout=15) as r:health=json.load(r)
  if health.get('status')!='up' or not REQUIRED_TOOLS<=set(health.get('published_tools',[])):
   p.error('MCP health/tools check failed')
  for label,field in [('graph','graph_path'),('config','config_path')]:
   live=Path(health.get(field,''))
   if not live.is_file() or sha(live)!=provenance[label+'_sha256']:
    p.error('Running server '+label+' does not match hashed external runtime')
 failed=False
 for task in tasks:
  for variant in variants:
   prompt=(root/task['prompt_dir']/f'{variant}.txt').read_text()
   # Agent sees only an opaque task ID, never the evaluator's CWE/sample ID.
   analysis_id='trial-'+hashlib.sha256((a.run_id+'/'+task['task_id']).encode()).hexdigest()[:24]
   prompt+='\nUse analysis_id="'+analysis_id+'" for this task.\n' if variant=='llm_mcp' else ''
   dest=root/'generated-code'/a.run_id/variant/task['sample_id']
   with tempfile.TemporaryDirectory(prefix='clear-codex-') as tmp:
    work=Path(tmp);(work/'prompt.txt').write_text(prompt);shutil.copy2(schema,work/'output.schema.json')
    container_name='clear-trial-'+uuid.uuid4().hex[:12]
    cmd=['docker','run','-i','--name',container_name,'--rm','--init','--read-only','--cap-drop=ALL','--security-opt=no-new-privileges',
      '--tmpfs','/tmp:rw,nosuid,nodev,size=128m','--add-host','host.docker.internal:host-gateway',
      '--mount',f'type=volume,src={a.auth_volume},dst=/home/node/.codex',
      '--mount',f'type=bind,src={work},dst=/work',a.image,
      '--no-daemon','exec','--ignore-user-config','--ephemeral','--skip-git-repo-check',
      '--model',a.model,'-c',f'model_reasoning_effort="{a.reasoning_effort}"','-s','read-only',
      '--json','--output-schema','/work/output.schema.json','-o','/work/final.json']
    if variant=='llm_mcp':
     cmd+=['-c','mcp_servers.vckg.url='+json.dumps(a.mcp_url),'-c','mcp_servers.vckg.required=true',
           '-c','mcp_servers.vckg.enabled_tools=["vckg_search_root_causes","vckg_search_fix_intents"]',
           '-c','mcp_servers.vckg.tools.vckg_search_root_causes.approval_mode="approve"',
           '-c','mcp_servers.vckg.tools.vckg_search_fix_intents.approval_mode="approve"',
           '-c','mcp_servers.vckg.startup_timeout_sec=120','-c','mcp_servers.vckg.tool_timeout_sec=180']
    cmd+=['-']
    if a.dry_run:
     # Replace ephemeral host path with a stable placeholder in the plan.
     print(json.dumps({'task':task['task_id'],'sample_id':task['sample_id'],'variant':variant,'destination':str(dest),'output_exists':dest.exists(),
       'command':[x.replace(str(work),'<isolated-task-dir>').replace(container_name,'<trial-container>') for x in cmd],
       'visible_files':['prompt.txt','output.schema.json'],'prompt_sha256':hashlib.sha256(prompt.encode()).hexdigest(),
       'vckg_provenance':provenance if variant=='llm_mcp' else {}},ensure_ascii=False))
     continue
    # Linux container user is uid 1000; allow it to create final.json in this scratch directory.
    work.chmod(0o777)
    dest.mkdir(parents=True);(dest/'prompt.txt').write_text(prompt)
    meta={'task_id':task['task_id'],'sample_id':task['sample_id'],'variant':variant,'model':a.model,
      'reasoning_effort':a.reasoning_effort,'image':a.image,'command':cmd,'timeout_seconds':a.timeout,
      'started_utc':datetime.datetime.now(datetime.timezone.utc).isoformat(),
      'prompt_sha256':sha(dest/'prompt.txt'),'schema_sha256':sha(schema),
      'data_manifest_sha256':sha(root/'generated-code/data/manifest.json'),
      'runner_sha256':sha(Path(__file__)),'task_manifest_sha256':sha(task_manifest),
      'vckg_provenance':provenance if variant=='llm_mcp' else {},'health_snapshot':health if variant=='llm_mcp' else None}
    meta['codex_version']=version;meta['image_id']=image_id
    with (dest/'events.jsonl').open('w') as stdout,(dest/'stderr.log').open('w') as stderr:
     try:
      result=subprocess.run(cmd,input=prompt,text=True,stdout=stdout,stderr=stderr,timeout=a.timeout)
      meta['exit_code']=result.returncode
     except KeyboardInterrupt:
      subprocess.run(['docker','rm','-f',container_name],capture_output=True,timeout=30)
      raise
     except subprocess.TimeoutExpired:
      meta['exit_code']=124;meta['status']='timeout'
      subprocess.run(['docker','rm','-f',container_name],capture_output=True,timeout=30)
    if (work/'final.json').exists():shutil.copy2(work/'final.json',dest/'final.json')
    audit=audit_events(dest/'events.jsonl',variant)
    (dest/'mcp_audit.json').write_text(json.dumps(audit,ensure_ascii=False,indent=2)+'\n')
    try:
     final=json.loads((dest/'final.json').read_text())
     if set(final)!={'code','imports','dependency_notes','retrieval_notes'} or not all(isinstance(v,str) for v in final.values()) or not final['code'].strip():raise ValueError('Invalid final schema')
     (dest/'code.java').write_text(final['imports'].strip()+'\n\n'+final['code'].strip()+'\n')
     meta['code_sha256']=sha(dest/'code.java')
     meta['status']='generation_recorded_unscored' if meta['exit_code']==0 and audit['valid'] else 'invalid_run'
    except (OSError,ValueError) as e:
     meta['status']='invalid_run';meta['output_error']=str(e)
    meta['finished_utc']=datetime.datetime.now(datetime.timezone.utc).isoformat()
    (dest/'run.json').write_text(json.dumps(meta,ensure_ascii=False,indent=2)+'\n')
    failed|=meta['status']!='generation_recorded_unscored'
    print(task['task_id'],variant,meta['status'],flush=True)
 if failed:raise SystemExit(1)
if __name__=='__main__':main()
