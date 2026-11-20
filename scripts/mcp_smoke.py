#!/usr/bin/env python3
"""Run using the external VCKG venv. Queries MCP, never calls a generative model."""
import argparse
import asyncio
import json
import uuid
from mcp import ClientSession
try:
 from mcp.client.streamable_http import streamable_http_client
except ImportError:
 from mcp.client.streamable_http import streamablehttp_client as streamable_http_client

async def smoke(url):
 async with streamable_http_client(url) as (read,write,_):
  async with ClientSession(read,write) as session:
   await session.initialize()
   tools=await session.list_tools()
   available={x.name for x in tools.tools}
   required={'vckg_search_root_causes','vckg_search_fix_intents'}
   if not required<=available:raise RuntimeError('Required tools are missing')
   result=await session.call_tool('vckg_search_root_causes',{'query':'An application parses caller supplied XML with a DocumentBuilder and returns a document','analysis_id':'smoke-'+uuid.uuid4().hex[:12]})
   if result.isError:raise RuntimeError(result.model_dump_json())
   print(json.dumps({'tools':sorted(available),'search':result.model_dump(mode='json')},ensure_ascii=False,indent=2))

if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('--url',default='http://127.0.0.1:4250/mcp');a=p.parse_args();asyncio.run(smoke(a.url))
