#!/usr/bin/env python3
"""Download the official Node Docker image with host curl, then docker load it.
Fallback when Docker's network cannot reach Docker Hub but host curl can.
TLS verification remains enabled. No Docker login credentials are read.
"""
import argparse
import hashlib
import json
import platform
import subprocess
import tarfile
import tempfile
from pathlib import Path

REGISTRY='https://registry-1.docker.io/v2/library/node'
ACCEPT='application/vnd.oci.image.index.v1+json, application/vnd.docker.distribution.manifest.list.v2+json, application/vnd.oci.image.manifest.v1+json, application/vnd.docker.distribution.manifest.v2+json'

def fetch(url,path,token=None):
 config='url = '+json.dumps(url)+'\n'
 if token:config+='header = '+json.dumps('Authorization: Bearer '+token)+'\n'
 config+='header = '+json.dumps('Accept: '+ACCEPT)+'\n'
 subprocess.run(['curl','--ipv4','--fail','--silent','--show-error','--location','--connect-timeout','20','--max-time','300','--retry','3','--config','-','--output',str(path)],input=config,text=True,check=True)

def verified(path,digest):
 h=hashlib.sha256()
 with path.open('rb') as f:
  for block in iter(lambda:f.read(1024*1024),b''):h.update(block)
 if digest!='sha256:'+h.hexdigest():raise ValueError('Digest mismatch: '+path.name)

def main():
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('--tag',default='22-bookworm-slim');a=p.parse_args()
 arch=subprocess.check_output(['docker','info','--format','{{.Architecture}}'],text=True).strip()
 arch={'aarch64':'arm64','x86_64':'amd64'}.get(arch,arch)
 with tempfile.TemporaryDirectory(prefix='official-node-image-') as tmp:
  root=Path(tmp);tokenpath=root/'token.json'
  fetch('https://auth.docker.io/token?service=registry.docker.io&scope=repository:library/node:pull',tokenpath)
  token=json.loads(tokenpath.read_text())['token'];tokenpath.unlink()
  index=root/'index.json';fetch(REGISTRY+'/manifests/'+a.tag,index,token);manifest=json.loads(index.read_text())
  if 'manifests' in manifest:
   desc=next(x for x in manifest['manifests'] if x.get('platform',{}).get('os')=='linux' and x['platform'].get('architecture')==arch)
   fetch(REGISTRY+'/manifests/'+desc['digest'],index,token);verified(index,desc['digest']);manifest=json.loads(index.read_text())
  config=root/'config.json';fetch(REGISTRY+'/blobs/'+manifest['config']['digest'],config,token);verified(config,manifest['config']['digest'])
  layers=[]
  for i,layer in enumerate(manifest['layers']):
   print(f'Downloading official Node layer {i+1}/{len(manifest["layers"])} ({layer["size"]//1024} KiB)',flush=True)
   name=f'layer-{i}.tar.gz';target=root/name;fetch(REGISTRY+'/blobs/'+layer['digest'],target,token);verified(target,layer['digest']);layers.append(name)
  (root/'manifest.json').write_text(json.dumps([{'Config':'config.json','RepoTags':['node:'+a.tag],'Layers':layers}]))
  archive=root/'image.tar'
  with tarfile.open(archive,'w') as tar:
   for name in ['config.json','manifest.json']+layers:tar.add(root/name,arcname=name)
  subprocess.run(['docker','load','--input',str(archive)],check=True)
 print('Loaded official node:'+a.tag+' for linux/'+arch)
if __name__=='__main__':main()
