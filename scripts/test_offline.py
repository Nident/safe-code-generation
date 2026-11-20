"""Offline tests: no Docker daemon, MCP server or model required."""
import argparse
import contextlib
import io
import json
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path
from unittest import mock
from prepare_data import build, method_fragment, sha
from run_generation import audit_events, main as run_main
from validate_data import validate

ROOT=Path(__file__).resolve().parents[1]
WORKSPACE=ROOT.parent

class OfflineTests(unittest.TestCase):
 def audit(self,events,variant='llm_mcp'):
  with tempfile.TemporaryDirectory() as tmp:
   path=Path(tmp)/'events';path.write_text('\n'.join(json.dumps(e) for e in events));return audit_events(path,variant)
 def calls(self):
  return [{'type':'item.completed','item':{'type':'mcp_tool_call','server':'vckg','tool':tool,'status':'completed',
   'arguments':{'query':'Parse user supplied XML','analysis_id':'task-01'},
   'result':{'content':[{'type':'text','text':'{"status":"no_relevant_match"}'}]},'error':None}}
   for tool in ['vckg_search_root_causes','vckg_search_fix_intents']]
 def test_real_calls_required_and_empty_matches_allowed(self):
  audit=self.audit(self.calls());self.assertTrue(audit['valid']);self.assertEqual(audit['no_relevant_match_count'],2)
  self.assertFalse(self.audit([{'type':'item.completed','item':{'type':'agent_message','text':'I called VCKG'}}])['valid'])
 def test_tool_error_identifier_mixed_ids_and_shell_rejected(self):
  for modify in ['error','identifier','mixed','shell','other_server']:
   events=self.calls()
   if modify=='error':events[0]['item']['result']['isError']=True
   if modify=='identifier':events[0]['item']['arguments']['query']='CWE-611'
   if modify=='mixed':events[0]['item']['arguments']['analysis_id']='different'
   if modify=='shell':events.append({'type':'item.completed','item':{'type':'command_execution'}})
   if modify=='other_server':events[0]['item']['server']='elsewhere'
   self.assertFalse(self.audit(events)['valid'],modify)
  self.assertFalse(self.audit(self.calls(),'llm')['valid'])
  self.assertTrue(self.audit([],'llm')['valid'])
 def test_method_extraction_handles_comments_strings_and_newlines(self):
  text='class X { private boolean f(String s)\n throws Exception { String x="}"; /* } */ return true; } void g() {} }'
  part,first,last=method_fragment(text,'private boolean f(String s) throws Exception')
  self.assertIn('return true;',part);self.assertNotIn('void g',part);self.assertEqual((first,last),(1,2))
 def test_current_inventory(self):
  with contextlib.redirect_stdout(io.StringIO()):validate(ROOT)
 def test_dry_run_has_no_external_calls_and_supports_spaces(self):
  with tempfile.TemporaryDirectory(prefix='clear path with spaces ') as tmp:
   copy=Path(tmp)/'experiment root';copy.mkdir()
   # Symlinks are only used by this host-side test, never mounted to an agent.
   for folder in ['generated-code','prompts']:(copy/folder).symlink_to(ROOT/folder,target_is_directory=True)
   output=io.StringIO()
   with mock.patch.object(sys,'argv',['run_generation','--root',str(copy),'--model','offline-test-model','--variant','llm','--samples','152,197','--dry-run']),mock.patch('subprocess.run',side_effect=AssertionError('External call during dry run')),contextlib.redirect_stdout(output):
    run_main()
   rows=[json.loads(line) for line in output.getvalue().splitlines()]
   self.assertEqual(len(rows),2)
   for row in rows:
    self.assertEqual(row['visible_files'],['prompt.txt','output.schema.json'])
    self.assertNotIn('mcp_servers',str(row['command']))
    self.assertIn('experiment root',row['destination'])
 def test_runner_records_a_simulated_trial_without_model(self):
  with tempfile.TemporaryDirectory(prefix='trial root with spaces ') as tmp:
   root=Path(tmp)
   (root/'prompts').symlink_to(ROOT/'prompts',target_is_directory=True)
   (root/'generated-code/data').mkdir(parents=True)
   for file in ['pilot_v1.json','manifest.json']:(root/'generated-code/data'/file).write_bytes((ROOT/'generated-code/data'/file).read_bytes())
   def fake_run(cmd,**kwargs):
    if cmd[-1]=='--version':return subprocess.CompletedProcess(cmd,0,'codex-cli offline-test\n','')
    if cmd[1:3]==['image','inspect']:return subprocess.CompletedProcess(cmd,0,'sha256:offline-test\n','')
    self.assertIn('--ignore-user-config',cmd);self.assertIn('--ephemeral',cmd)
    mounts=[cmd[i+1] for i,x in enumerate(cmd) if x=='--mount']
    bind=next(m for m in mounts if m.startswith('type=bind'))
    work=Path(bind.removeprefix('type=bind,src=').removesuffix(',dst=/work'))
    self.assertEqual({f.name for f in work.iterdir()},{'prompt.txt','output.schema.json'})
    (work/'final.json').write_text(json.dumps(dict(code='void example() {}',imports='',dependency_notes='synthetic test',retrieval_notes='')))
    kwargs['stdout'].write(json.dumps({'type':'turn.completed'})+'\n')
    return subprocess.CompletedProcess(cmd,0)
   with mock.patch.object(sys,'argv',['run_generation','--root',str(root),'--model','offline-test-model','--variant','llm','--samples','152']),mock.patch('subprocess.run',side_effect=fake_run),contextlib.redirect_stdout(io.StringIO()):run_main()
   run=next((root/'generated-code/v1/llm').glob('*/run.json'))
   meta=json.loads(run.read_text());self.assertEqual(meta['status'],'generation_recorded_unscored')
   self.assertEqual(meta['code_sha256'],sha(run.with_name('code.java')))
 def test_rebuild_reproducible_and_corruption_detected(self):
  # Raw sources are optional after Git transfer; skip just this reproduction test there.
  vckg=WORKSPACE/'VCKG';clear=vckg/'CLEAR_Causal_Context_based_Agentic_Reasoning_for_Vulnerability_Detection2/dataset/inintCLEARdataset/java-pair/java_data_paired.json'
  if not clear.exists():self.skipTest('Original external VCKG sources not present')
  with tempfile.TemporaryDirectory(prefix='prepared path with spaces ') as tmp:
   out=Path(tmp)/'output root'
   args=argparse.Namespace(output=out,clear=clear,train=vckg/'java-pair/java_train_paired_PROJECT_LVL_NEW_TEST_LEAKAGE_FREE.json',secbench=ROOT/'bechmarks/sec-code-bench',graph=vckg/'Vuln_agent/vckg/knowledge_graph/project_lvl_final_knowledge_graph.json',vckg_config=vckg/'Vuln_agent/vckg/vckg_tool_config.yaml',replace=False)
   with contextlib.redirect_stdout(io.StringIO()):build(args)
   first=sha(out/'generated-code/data/manifest.json')
   args.replace=True
   with contextlib.redirect_stdout(io.StringIO()):build(args)
   self.assertEqual(first,sha(out/'generated-code/data/manifest.json'))
   self.assertEqual(first,sha(ROOT/'generated-code/data/manifest.json'))
   (out/'prompts/v1/task-01/task.md').write_text('corrupted')
   with self.assertRaisesRegex(AssertionError,'Hash mismatch'):validate(out)

if __name__=='__main__':unittest.main()
