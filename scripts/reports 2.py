import urllib.request,json,random
def api(m,p,d=None,t=None):
 u=f"http://127.0.0.1:8080/api{p}"
 h={"Content-Type":"application/json"}
 if t:h["Authorization"]=f"Bearer {t}"
 b=json.dumps(d).encode() if d else None
 r=urllib.request.Request(u,data=b,headers=h,method=m)
 try:return json.loads(urllib.request.urlopen(r).read())
 except:return None
def login(p):
 for pw in ["Aa123456!","Abc123!"]:
  r=api("POST","/user/login",{"phone":p,"password":pw})
  if r and r.get("code")==200:return r["data"]["token"]
 return None
admin_r=api("POST","/user/login",{"phone":"13800000003","password":"Abc123!"})
admin=admin_r["data"]["token"]
r=api("GET","/admin/reviews?page=1&size=200",t=admin)
rc=0
if r and r.get("code")==200:
 d=r.get("data",{});revs=d.get("records",[]) if isinstance(d,dict) else (d if isinstance(d,list) else [])
 for rv in random.sample(revs,min(30,len(revs))):
  rid=rv.get("id");uid=rv.get("userId","")
  if not rid:continue
  for i in range(20,80):
   if f"13800000{i:03d}"==uid:continue
   t=login(f"13800000{i:03d}")
   if not t:continue
   rr=api("POST",f"/review/{rid}/report",{"reason":"内容不当"},t=t)
   if rr and rr.get("code")==200:rc+=1;break
print(f"Reports: {rc}")
