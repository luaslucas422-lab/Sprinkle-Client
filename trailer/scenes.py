"""Blender scenes for the Sprinkle Client trailer. Usage: python3 scenes.py <scene> <start> <end> <outdir>"""
import sys, math, random, os
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from scene_lib import *

def ease(t): return t*t*(3-2*t)

def scene_drop():
    """A donut falls onto a superflat world, bounces, and turns to the camera. 4 s."""
    s=reset(); ground(); sun(4,(0.9,0.1,0.5))
    d=donut((0,0,12),1.2,'hero',80)
    cam,tgt=camera((0,-11,1.8),(0,0,2))
    # fall, bounce, settle standing up facing camera
    key(d,1,location=(0,0,14),rotation_euler=(0.3,0.2,0))
    key(d,22,location=(0,0,1.0),rotation_euler=(0.1,0.1,1.2))
    key(d,32,location=(0,0,3.2),rotation_euler=(0.9,0,-0.8))
    key(d,44,location=(0,0,1.0),rotation_euler=(1.3,0,-0.3))
    key(d,60,location=(0,0,2.1),rotation_euler=(math.pi/2,0,0))
    key(d,120,location=(0,0,2.1),rotation_euler=(math.pi/2,0,0.3))
    key(cam,1,location=(0,-11,1.8)); key(cam,120,location=(1.8,-9,2.2))
    s.frame_end=120

def spawner(loc,m_cage,m_core):
    c=cube(loc,1.0,m_cage,'cage')
    w=c.modifiers.new('wire','WIREFRAME'); w.thickness=0.08
    cube(loc,0.45,m_core,'core')

def scene_spawners():
    """Spawners stack up and coins fly out. 4 s."""
    s=reset(); ground(); sun(3,(0.7,0.3,0.9))
    cage=mat('cage',(0.05,0.07,0.1,1),0.4); core=mat('core',(1,0.35,0.05,1),0.5,emit=1.2)
    gold=mat('gold',(1,0.7,0.1,1),0.25)
    gold.node_tree.nodes['Principled BSDF'].inputs['Metallic'].default_value=1
    objs=[]
    for i,(x,y) in enumerate([(-1.1,0),(0,0),(1.1,0),(-0.55,0),(0.55,0),(0,0)]):
        z=0.5 if i<3 else (1.6 if i<5 else 2.7)
        if i>=3: x = (-0.55,0.55,0)[i-3]
        before=set(bpy.data.objects)
        spawner((x,y,z),cage,core)
        new=[o for o in bpy.data.objects if o not in before]
        for o in new:
            key(o,1+i*8,location=(o.location.x,o.location.y,z+8))
            key(o,10+i*8,location=(o.location.x,o.location.y,z))
    r=random.Random(3)
    for i in range(40):
        bpy.ops.mesh.primitive_cylinder_add(radius=0.18,depth=0.05,location=(0,0,2.7)); c=bpy.context.object; c.data.materials.append(gold)
        a=r.random()*math.tau; sp=r.uniform(2,5); f0=60+r.randint(0,10)
        key(c,1,scale=(0,0,0)); key(c,f0,scale=(0,0,0),location=(0,0,2.7))
        key(c,f0+1,scale=(1,1,1))
        key(c,f0+45,location=(math.cos(a)*sp,math.sin(a)*sp*0.6,2.7+r.uniform(1,3)),rotation_euler=(r.random()*6,r.random()*6,0))
    camera((5.5,-8.5,3.6),(0,0,1.9))
    cam=bpy.context.scene.camera; key(cam,1,location=(5.5,-8.5,3.6)); key(cam,120,location=(3.2,-9.5,3.0))
    s.frame_end=120

def scene_pig():
    """A blocky frosted pig hops in front of the camera. 4 s."""
    s=reset(); ground(); sun(4,(0.8,0.2,0.4))
    fro=mat('pfro',FROST,0.35); dough=mat('pd',DOUGH,0.7); pink=mat('snout',(1,0.4,0.6,1),0.5)
    white=mat('eye',(1,1,1,1),0.3); black=mat('pupil',(0,0,0,1),0.3)
    bpy.ops.object.empty_add(location=(0,0,0)); root=bpy.context.object
    parts=[cube((0,0,1.0),(1.0,1.6,0.8),fro), cube((0,0,0.92),(1.02,1.62,0.18),dough),
           cube((0,-1.05,1.25),(0.8,0.6,0.8),fro), cube((0,-1.38,1.15),(0.4,0.08,0.3),pink),
           cube((-0.22,-1.36,1.45),(0.16,0.04,0.16),white), cube((0.22,-1.36,1.45),(0.16,0.04,0.16),white),
           cube((-0.2,-1.39,1.43),(0.07,0.02,0.07),black), cube((0.24,-1.39,1.43),(0.07,0.02,0.07),black)]
    for x in (-0.3,0.3):
        for y in (-0.5,0.5): parts.append(cube((x,y,0.3),(0.35,0.35,0.6),fro))
    r=random.Random(5); sm=[mat(f'ps{i}',c,0.4) for i,c in enumerate(SPR)]
    for i in range(30):
        parts.append(cube((r.uniform(-0.45,0.45),r.uniform(-0.75,0.75),1.41),(0.12,0.03,0.03),sm[i%6]))
    for p in parts: p.parent=root
    for i,f in enumerate(range(1,121,20)):
        key(root,f,location=(0,0,0),rotation_euler=(0,0,0.15*((-1)**i)))
        key(root,f+10,location=(0,0,0.6),rotation_euler=(0,0,0))
    camera((2.6,-5.5,1.6),(0,-0.5,1.0))
    s.frame_end=120

def scene_outro():
    """Donuts orbit the hero donut while the camera pulls back. 5 s."""
    s=reset(); ground(); sun(3.5,(0.9,0.2,0.3))
    hero=donut((0,0,2.4),1.3,'hero',80); hero.rotation_euler=(math.pi/2,0,0)
    key(hero,1,rotation_euler=(math.pi/2,0,-0.4)); key(hero,150,rotation_euler=(math.pi/2,0,0.4))
    bpy.ops.object.empty_add(location=(0,0,2.4)); ring=bpy.context.object
    for i in range(6):
        a=i/6*math.tau; d=donut((math.cos(a)*3.0,math.sin(a)*3.0,0.6*math.sin(a*2)),0.4,f'o{i}',20,seed=i+7)
        d.parent=ring; d.rotation_euler=(0.6,0.3,a)
    key(ring,1,rotation_euler=(0,0,0)); key(ring,150,rotation_euler=(0,0,math.pi*0.8))
    cam,_=camera((0,-8.5,3.0),(0,0,2.4)); key(cam,1,location=(0,-8.5,3.0)); key(cam,150,location=(0,-13,4.2))
    s.frame_end=150

SCENES={'drop':scene_drop,'spawners':scene_spawners,'pig':scene_pig,'outro':scene_outro}

if __name__=='__main__':
    name,start,end,out=sys.argv[1],int(sys.argv[2]),int(sys.argv[3]),sys.argv[4]
    SCENES[name]()
    s=bpy.context.scene
    if len(sys.argv)>5: s.cycles.samples=int(sys.argv[5])
    end=min(end,s.frame_end)
    os.makedirs(out,exist_ok=True)
    for f in range(start,end+1):
        s.frame_set(f); s.render.filepath=f'{out}/{name}_{f:04d}.png'
        bpy.ops.render.render(write_still=True)
