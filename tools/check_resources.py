"""Validate shipped resource links against this exact Minecraft version's client JAR."""
from pathlib import Path
import json,zipfile,sys,struct,zlib
R=Path(__file__).resolve().parents[1]/'src/main/resources'
V=zipfile.ZipFile(sys.argv[1])
errors=[];count=0

def exists(name):return (R/name).is_file() or name in V.namelist()
def location(ref,kind,ext):
 ns,sep,path=ref.partition(':')
 if not sep:ns,path='minecraft',ns
 return f'assets/{ns}/{kind}/{path}.{ext}'
def check(ref,kind,ext,owner):
 if not ref.startswith('#') and not exists(location(ref,kind,ext)):errors.append(f'{owner}: missing {ref} ({kind})')
for p in (R/'assets/wildlands/models').rglob('*.json'):
 d=json.loads(p.read_text());count+=1
 if 'parent' in d:check(d['parent'],'models','json',p)
 for ref in d.get('textures',{}).values():check(ref,'textures','png',p)
 for e in d.get('elements',[]):
  for coord in e['from']+e['to']:
   if not -16<=coord<=32:errors.append(f'{p}: element outside supported model bounds')
for p in (R/'assets/wildlands/blockstates').glob('*.json'):
 d=json.loads(p.read_text());count+=1
 entries=list(d.get('variants',{}).values())+[x['apply'] for x in d.get('multipart',[])]
 for entry in entries:
  for a in entry if isinstance(entry,list) else [entry]:check(a['model'],'models','json',p)
for p in R.rglob('*.json'):json.loads(p.read_text())
for p in (R/'assets/wildlands/textures').rglob('*.png'):
 b=p.read_bytes();assert b[:8]==b'\x89PNG\r\n\x1a\n',p;offset=8;stream=b''
 while offset<len(b):
  size=struct.unpack('>I',b[offset:offset+4])[0];kind=b[offset+4:offset+8];data=b[offset+8:offset+8+size];crc=struct.unpack('>I',b[offset+8+size:offset+12+size])[0]
  assert zlib.crc32(kind+data)&0xffffffff==crc,p
  if kind==b'IDAT':stream+=data
  offset+=size+12
 zlib.decompress(stream);count+=1
assert json.loads((R/'data/wildlands/recipe/root_flower_banner_pattern.json').read_text())['result']['id']=='wildlands:root_flower_banner_pattern'
for p in (R/'data/wildlands/loot_table').rglob('*.json'):assert 'root_flower_banner_pattern' not in p.read_text(),p
assert len(list((R/'data/wildlands/worldgen/biome').glob('*.json')))==13
if errors:raise SystemExit('\n'.join(errors))
print(f'PASS: {count} model/blockstate/PNG resources, all JSON, thirteen biomes, and craft-only flower pattern.')
