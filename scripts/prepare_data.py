#!/usr/bin/env python3
"""Prepare a reproducible, reference-only CLEAR/Java SecCodeBench intersection."""
import argparse
import collections
import csv
import difflib
import hashlib
import json
import re
import shutil
import xml.etree.ElementTree as ET
from pathlib import Path
from pilot_specs import PILOTS

VERSION = '1.0.0'
WANTED = [79,352,22,862,611,522,20,200,295,502,400,863,770,287,285,345,918,89,94]
GROUPS = {
 'CWE-22': ['PathTraversalRead','PathTraversalWrite','ZipSlip'],
 'CWE-79': ['XSSSpringMVC'],
 'CWE-89': ['JDBCInjection','SQLInjectionMyBatisGroupBy','SQLInjectionMyBatisOrderBy','SQLInjectionSpringJDBC','SQLInjectionMyBatisLike','JDBCInjectionExecuteUpdate','SQLInjectionMyBatisCRUD'],
 'CWE-94': ['CodeInjectionGroovy','CodeInjectionJavaxScript'],
 'CWE-502': ['DeserializationHessian','DeserializationObjectInputStream','DeserializationSnakeYAML','DeserializationXMLDecoder','DeserializationFlexjson'],
 'CWE-611': ['XXESAXReader','XXESAXParser','XXEDocumentBuilder','XXEValidator','XXETransformerFactory','XXEJDOM2','XXEStAX','XXEDigester','XXEXPath','XXEJAXBUnmarshal','XXESchemaFactory','XXEXMLReader'],
 'CWE-918': ['SSRFApacheHttpClient','SSRFURLConnection','SSRFRestTemplate','SSRFCommonsIO','SSRFOkHttp','SSRFImageIO','SSRFJsoup'],
}
EXPECTED = dict(zip(GROUPS, [30,32,14,3,15,27,6]))
MECHANISMS = {
 'CWE-22':'User-controlled file paths or ZIP entry names escape an intended directory.',
 'CWE-79':'Untrusted content is rendered in an HTML response.',
 'CWE-89':'Untrusted values influence SQL query structure.',
 'CWE-94':'User-supplied source is evaluated by a scripting engine; transfer to XSLT requires an engine-specific attack.',
 'CWE-502':'Untrusted serialized input is reconstructed into Java objects.',
 'CWE-611':'XML processing can resolve external entities or resources.',
 'CWE-918':'User-controlled destinations cause server-side outbound requests.',
}
KNOWN_ISSUES = {
 3:'The visible diff changes error reporting, not the interpolated SQL sink.',
 19:'The visible operation is local path containment, despite the SSRF label.',
 95:'The visible change concerns authorization/context, not an outbound URL sink.',
 116:'The visible diff replaces bound parameters with concatenation: possible reversed pair.',
 178:'The visible change removes input from an error message; code execution is not established.',
 239:'The visible change works around a JDK compiler crash; code injection is not established.',
}
BASE_WRAPPER = '''Implement the Java task below from its specification. Produce one method implementation with the specified signature. Include required imports in imports, not in the method body. Do not create a replacement application or implement missing project helpers. List missing contracts in dependency_notes. Do not use shell, filesystem, web, other agents or external tools; use only the supplied specification. Return the required JSON object.\n'''
MCP_WRAPPER = '''Implement the Java task below from its specification. Before writing the code, call vckg_search_root_causes and vckg_search_fix_intents, using concise English descriptions of this task's behavior. Use the same opaque analysis_id for both calls. Do not use vulnerability identifiers in queries. Inspect the returned exemplar chains and decide which patterns apply to this task; treat them as prior knowledge, not proof. Both calls must complete successfully, even if a response is no_relevant_match. On a tool error, stop and report it; do not silently fall back. Summarize applicability and empty results in retrieval_notes; the runner records actual tool calls. Use no shell, filesystem, web, other agents or tools except the VCKG MCP. Produce one method implementation with the specified signature; put imports in imports and unresolved project contracts in dependency_notes. Return the required JSON object.\n'''

def sha(path):
 return hashlib.sha256(Path(path).read_bytes()).hexdigest()
def digest(text):
 return hashlib.sha256(text.encode()).hexdigest()
def norm(text):
 return re.sub(r'\s+', '', text)
def identifiers(obj, pattern):
 return set(re.findall(pattern, json.dumps(obj, ensure_ascii=False)))
def write_json(path, obj):
 path.parent.mkdir(parents=True, exist_ok=True)
 path.write_text(json.dumps(obj, ensure_ascii=False, indent=2)+'\n')
def sid(row):
 return f"{row['cwe_id']}_idx{row['index']:03d}_{row['commit_hash'][:12]}"
def method_fragment(text, signature):
 """Extract the selected method using Java string/comment-aware brace balancing."""
 match=re.search(r'\s+'.join(re.escape(part) for part in signature.split()),text)
 if not match:raise ValueError('Target signature missing: '+signature)
 start=match.start()
 brace=text.index('{',start)
 token=re.compile(r'//[^\n]*|/\*[\s\S]*?\*/|"(?:\\.|[^"\\])*"|\'(?:\\.|[^\'\\])*\'|[{}]')
 depth=0
 for m in token.finditer(text,brace):
  if m.group()=='{':depth+=1
  elif m.group()=='}':
   depth-=1
   if depth==0:return text[start:m.end()], text[:start].count('\n')+1, text[:m.end()].count('\n')+1
 raise ValueError('Unclosed method')

def build(args):
 out=args.output.resolve();data=out/'generated-code/data';prompts=out/'prompts/v1'
 if (data/'manifest.json').exists() and not args.replace:
  raise ValueError('Prepared data already exists; pass --replace to rebuild managed data/prompts.')
 # Only remove the two generated trees, and only with explicit --replace.
 if args.replace:
  for p in (data,prompts):
   if p.exists():shutil.rmtree(p)
 data.mkdir(parents=True,exist_ok=True);prompts.mkdir(parents=True,exist_ok=True)
 clear=json.loads(args.clear.read_text());train=json.loads(args.train.read_text())
 graph=json.loads(args.graph.read_text());bench=json.loads((args.secbench/'datasets/benchmark/java/java.json').read_text())
 selected=[r for r in clear if r['cwe_id'] in GROUPS]
 counts=collections.Counter(r['cwe_id'] for r in selected)
 if counts!=EXPECTED:raise ValueError(f'Source counts changed: {dict(counts)} != {EXPECTED}')
 train_cwes=identifiers([r.get('cwe_ids',r.get('cwe_id')) for r in train],r'CWE-\d+')
 assert set(GROUPS)<=train_cwes
 # Do not equate graph-local numerical indices to unrelated CLEAR or train indices.
 graph_ids=identifiers(graph,r'(?:CVE-\d{4}-\d+|GHSA-[\w-]+|\b[0-9a-f]{40}\b)')
 graph_entries=graph.get('data',[]) if isinstance(graph,dict) else graph
 graph_cwes=collections.Counter(e.get('root_cwe_id') for e in graph_entries)
 train_norm=[(r.get('index'),norm(r.get('func_before','')),norm(r.get('func_after',''))) for r in train]
 train_cv=[(r.get('index'),identifiers([r.get('cve_id'),r.get('cve_ids')],r'CVE-\d{4}-\d+')) for r in train]
 mappings=[];source_hashes={}
 for cwe, tasks in GROUPS.items():
  for task in tasks:
   cfg=bench[task];assert cfg['SecTester']=='UnitTester'
   rel=Path('datasets/templates/java')/cfg['template'];src=args.secbench/rel
   dst=data/'sec_code_bench/templates'/rel
   shutil.copytree(src,dst)
   sec=sorted(dst.rglob('SecurityTest.java'));func=sorted(dst.rglob('FunctionalTest.java'))
   assert sec and func and (dst/'pom.xml').is_file(),task
   texts='\n'.join(p.read_text() for p in sec)
   urls=sorted(set(re.findall(r'https?://[^\s"<>\\]+',texts)))
   urls=[u for u in urls if 'apache.org/licenses' not in u and 'www.w3.org/' not in u]
   fixture_text='\n'.join(f.read_text() for f in dst.rglob('*.java'))
   service_notes=[]
   if '127.0.0.1:7000' in texts:service_notes.append('External HTTP log/check service at 127.0.0.1:7000 is required in the test network namespace.')
   if 'HttpServer.create' in fixture_text or 'MockWebServer' in fixture_text:service_notes.append('Template starts an in-process HTTP fixture; loopback and dynamic ports must be available.')
   if 'jdbc:h2:' in fixture_text:service_notes.append('Embedded H2 fixture; Maven driver required, no external database inferred.')
   if 'Mockito' in texts or 'mockStatic' in texts:service_notes.append('Uses mocked dependencies: sink/argument assertions are not an end-to-end exploit.')
   if 'drsupport.oss-cn-beijing.aliyuncs.com' in texts:service_notes.append('Some redirect cases contact an external URL; unavailable network means not tested.')
   ns={'m':'http://maven.apache.org/POM/4.0.0'}
   pom=ET.parse(dst/'pom.xml')
   libs=[f"{dep.findtext('m:groupId',default='',namespaces=ns)}:{dep.findtext('m:artifactId',default='',namespaces=ns)}:{dep.findtext('m:version',default='inherited',namespaces=ns)}" for dep in pom.findall('.//m:dependencies/m:dependency',ns)]
   methods=re.findall(r'@Test(?:\([^)]*\))?\s+public\s+void\s+(\w+)',texts)
   evidence=[{'path':str(p.relative_to(data)), 'sha256':sha(p), 'test_methods':re.findall(r'public\s+void\s+(test\w+)',p.read_text())} for p in sec]
   mappings.append(dict(cwe=cwe,task_id=task,source_template=str(rel),template=str(dst.relative_to(data)),
    functional_tests=[str(p.relative_to(data)) for p in func],security_tests=[str(p.relative_to(data)) for p in sec],
    libraries=libs,mechanism=('JDBC connection URL parameter injection, not SQL query injection.' if task=='JDBCInjection' else MECHANISMS[cwe]),
    mapping_status='related_candidate_not_confirmed_cwe89' if task=='JDBCInjection' else 'category_match_requires_adapter',
    recommended_for_cwe=task!='JDBCInjection',mapping_basis='Curated semantic mapping from task purpose and local SecurityTest source, not an upstream CWE annotation.',
    evidence=evidence,required_services={'service_notes':service_notes or ['No external service inferred statically; review setup before execution.'],'observed_urls_in_security_tests':urls,'runtime_requirements':'Inspect test fixture setup and pom.xml; preserve benchmark HTTP logging/database dependencies. No claim of self-contained execution.'},
    upstream_config=cfg,compatibility='requires_adaptation',test_methods=methods))
   for f in sorted(src.rglob('*')):
    if f.is_file():source_hashes[str(f.relative_to(args.secbench))]=sha(f)
 shutil.copy2(args.secbench/'LICENSE',data/'sec_code_bench/LICENSE')
 write_json(data/'sec_code_bench/mapping.json',mappings)
 write_json(data/'sec_code_bench/source_file_hashes.json',source_hashes)
 wrapped=[];pilot=[]
 for row in selected:
  sample_id=sid(row);folder=data/'samples'/row['cwe_id']/sample_id;folder.mkdir(parents=True)
  before=row['func_before'];after=row['func_after'];normalized=norm(before)
  cvs=identifiers([row.get('cve_id')],r'CVE-\d{4}-\d+')
  leakage=dict(train_commit_indices=[r.get('index') for r in train if r.get('commit_hash')==row['commit_hash']],
   train_cve_indices=[i for i,cs in train_cv if cvs&cs],
   train_normalized_code_indices=[i for i,b,a in train_norm if normalized and normalized in (b,a)],
   train_code_containment_indices=[i for i,b,a in train_norm if len(normalized)>=100 and (normalized in b or normalized in a)],
   graph_explicit_identifier_matches=sorted((cvs|{row['commit_hash']})&graph_ids),
   graph_audit_limit='Graph entries expose local index/root_cwe_id/nodes/edges. Local indices are not provenance IDs. Absence of an explicit match does not prove independence.')
  leakage['known_overlap']=any(leakage[k] for k in ['train_commit_indices','train_cve_indices','train_normalized_code_indices','train_code_containment_indices','graph_explicit_identifier_matches'])
  spec=PILOTS.get(row['index']);issues=[]
  if row['index'] in KNOWN_ISSUES:issues.append(KNOWN_ISSUES[row['index']])
  if norm(before)==norm(after):issues.append('No visible code change after whitespace normalization.')
  if not before.strip() or not after.strip():issues.append('Missing before/after code.')
  if spec:issues.append(spec['fix_review'])
  metadata=dict(sample_id=sample_id,cwe=row['cwe_id'],source_index=row['index'],
   attack_candidates=[t for t in GROUPS[row['cwe_id']] if t!='JDBCInjection'],compatibility='requires_adaptation',leakage=leakage,
   quality=dict(review_status=spec['review'] if spec else 'unreviewed_source_label_only',issues=issues,
    context_completeness='fragment_only_imports_enclosing_class_or_project_dependencies_may_be_missing',
    external_dependencies_review=spec['context'] if spec else 'Not resolved; inspect source fragment and original project before compilation.',
    func_after_oracle='unvalidated_source_after_not_guaranteed_secure'),
   diff_origin='source' if 'func_diff' in row else 'derived_unified_diff_source_field_absent',
   original_metadata={k:v for k,v in row.items() if k not in ['func_before','func_after','func_diff']})
  for name,key in [('func_before.java','func_before'),('func_after.java','func_after'),('func.diff','func_diff')]:
   value=row[key] if key in row else ''.join(difflib.unified_diff(before.splitlines(keepends=True),after.splitlines(keepends=True),fromfile='func_before',tofile='func_after'))
   (folder/name).write_text(value)
  write_json(folder/'metadata.json',metadata)
  wrapped.append(dict(sample_id=sample_id,source=row,prepared=metadata))
  if spec:
   opaque=f"task-{len(pilot)+1:02d}";pf=prompts/opaque;pf.mkdir()
   task=f"Task: {opaque}\n\nSignature:\n{spec['signature']}\n\nFunctional requirements:\n{spec['description']}\n\nAvailable context and dependencies:\n{spec['context']}\n"
   (pf/'task.md').write_text(task)
   for variant,wrapper in [('llm',BASE_WRAPPER),('llm_mcp',MCP_WRAPPER)]:
    (pf/f'{variant}.txt').write_text(wrapper+'\n'+task)
   fragments={}
   for key in ['func_before','func_after']:
    frag,first,last=method_fragment(row[key],spec['signature']);(folder/f'target_{key}.java').write_text(frag)
    fragments[key]={'path':str((folder/f'target_{key}.java').relative_to(data)),'source_lines':[first,last]}
   pilot.append(dict(task_id=opaque,sample_id=sample_id,cwe=row['cwe_id'],source_index=row['index'],
    prompt_dir=str(pf.relative_to(out)),reference_dir=str(folder.relative_to(out)),target=fragments,
    mechanism_review=spec['review'],evidence=spec['evidence'],preferred_attack_candidates=spec['preferred'],next_adaptation_step=spec['adaptation'],
    generation_enabled=True,mechanism_confirmed=spec.get('eligible',True),dynamic_scoring_eligible=False,
    eligibility_reason='No executable adapter or before/after differential validation yet.' if spec.get('eligible',True) else 'URI validation/fix context is missing; generation-only exploratory task.',
    leakage=leakage))
 (data/'clear_samples.jsonl').write_text(''.join(json.dumps(r,ensure_ascii=False)+'\n' for r in wrapped))
 write_json(data/'pilot_v1.json',pilot)
 schema={'type':'object','additionalProperties':False,'properties':{'code':{'type':'string'},'imports':{'type':'string'},'dependency_notes':{'type':'string'},'retrieval_notes':{'type':'string'}},'required':['code','imports','dependency_notes','retrieval_notes']}
 write_json(prompts/'output.schema.json',schema)
 with (data/'intersection_summary.csv').open('w',newline='') as f:
  w=csv.writer(f);w.writerow(['cwe','clear_records','secbench_copied_tasks','secbench_confirmed_category_tasks','train_records','graph_entries'])
  for cwe in GROUPS:w.writerow([cwe,counts[cwe],len(GROUPS[cwe]),len([t for t in GROUPS[cwe] if t!='JDBCInjection']),sum(cwe in identifiers([r.get('cwe_id'),r.get('cwe_ids')],r'CWE-\d+') for r in train),graph_cwes[cwe]])
 all_counts=collections.Counter(r['cwe_id'] for r in clear)
 with (data/'excluded_cwes.csv').open('w',newline='') as f:
  w=csv.writer(f);w.writerow(['cwe','clear_records','reason'])
  for n in WANTED:
   cwe=f'CWE-{n}'
   if cwe not in GROUPS:w.writerow([cwe,all_counts[cwe],'Absent from source CLEAR' if not all_counts[cwe] else 'Java security assessment is LLM-based, no executable attack' if n==200 else 'No confirmed matching Java dynamic task in pinned local sec-code-bench'])
 sources={name:{'filename':path.name,'sha256':sha(path)} for name,path in [('clear',args.clear),('train',args.train),('graph',args.graph),('vckg_config',args.vckg_config),('secbench_java_config',args.secbench/'datasets/benchmark/java/java.json')]}
 manifest=dict(preparation_version=VERSION,sources=sources,counts={'clear_source':len(clear),'train_source':len(train),'selected_clear':len(selected),'secbench_tasks':len(mappings),'secbench_confirmed_category_tasks':sum(m['recommended_for_cwe'] for m in mappings),'pilot_prompts':len(pilot)},
  graph_audit={'entries':len(graph_entries),'explicit_advisory_or_commit_ids_found':len(graph_ids),'local_index_join_performed':False,'limitation':'No provenance equivalence can be inferred from local numeric indices.'},
  overlap_records=sum(r['prepared']['leakage']['known_overlap'] for r in wrapped),
  generator_sha256=sha(Path(__file__)),pilot_specs_sha256=sha(Path(__file__).with_name('pilot_specs.py')))
 shutil.copy2(Path(__file__).resolve().parents[1]/'experiment/DATA_README.md', data/'README.md')
 # Portable inventory also covers prompts; it is generated last and excludes itself.
 manifest['files']={str(p.relative_to(out)):sha(p) for root in (data,prompts) for p in sorted(root.rglob('*')) if p.is_file() and p.name!='manifest.json'}
 write_json(data/'manifest.json',manifest)
 print(json.dumps(manifest['counts'],ensure_ascii=False));print('Known-overlap records:',manifest['overlap_records'])

def main():
 p=argparse.ArgumentParser(description=__doc__)
 for name in ['clear','train','secbench','graph','vckg-config','output']:p.add_argument('--'+name,type=Path,required=True)
 p.add_argument('--replace',action='store_true');args=p.parse_args()
 build(args)
if __name__=='__main__':main()
