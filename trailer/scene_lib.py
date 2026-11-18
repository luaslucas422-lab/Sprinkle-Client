import bpy, math, random

FROST=(0.003,0.08,0.9,1); DOUGH=(0.43,0.21,0.08,1); OUT=(0.0,0.003,0.03,1)
SPR=[(1,0.16,0.43,1),(1,0.75,0.1,1),(0.2,1,0.25,1),(1,1,1,1),(1,0.35,0.05,1),(0.5,0.25,1,1)]

def reset(w=1280,h=720,samples=24):
    bpy.ops.wm.read_factory_settings(use_empty=True)
    s=bpy.context.scene
    s.render.engine='CYCLES'; s.cycles.device='CPU'; s.cycles.samples=samples
    s.cycles.use_denoising=True
    s.render.resolution_x=w; s.render.resolution_y=h; s.render.fps=30
    s.view_settings.view_transform='Standard'; s.view_settings.look='Medium High Contrast'
    w_=bpy.data.worlds.new('w'); s.world=w_; w_.use_nodes=True
    bg=w_.node_tree.nodes['Background']; bg.inputs[0].default_value=(0.3,0.6,1.0,1); bg.inputs[1].default_value=0.9
    return s

def mat(name,col,rough=0.5,emit=0):
    m=bpy.data.materials.new(name); m.use_nodes=True
    b=m.node_tree.nodes['Principled BSDF']; b.inputs['Base Color'].default_value=col; b.inputs['Roughness'].default_value=rough
    if emit: b.inputs['Emission Color'].default_value=col; b.inputs['Emission Strength'].default_value=emit
    return m

def cube(loc,size,m,name='c'):
    bpy.ops.mesh.primitive_cube_add(size=1,location=loc); o=bpy.context.object; o.name=name
    o.scale=size if isinstance(size,tuple) else (size,size,size); o.data.materials.append(m); return o

def ground(n=24):
    grass=mat('grass',(0.16,0.45,0.08,1),0.9); dirt=mat('dirt',(0.3,0.17,0.08,1),0.95)
    bpy.ops.mesh.primitive_plane_add(size=n*2,location=(0,0,0)); p=bpy.context.object; p.data.materials.append(grass)
    # block grid lines via a slightly darker checker
    tex=grass.node_tree.nodes.new('ShaderNodeTexChecker'); tex.inputs['Scale'].default_value=n*2
    tex.inputs['Color1'].default_value=(0.16,0.45,0.08,1); tex.inputs['Color2'].default_value=(0.14,0.40,0.07,1)
    grass.node_tree.links.new(tex.outputs['Color'], grass.node_tree.nodes['Principled BSDF'].inputs['Base Color'])
    return p

def donut(loc=(0,0,0),scale=1.0,name='donut',sprinkles=60,seed=1):
    bpy.ops.mesh.primitive_torus_add(major_radius=1,minor_radius=0.45,major_segments=64,minor_segments=24,location=loc)
    d=bpy.context.object; d.name=name; d.data.materials.append(mat('dough'+name,DOUGH,0.7))
    bpy.ops.object.shade_smooth()
    # frosting: a slightly bigger torus, cut to the top half
    bpy.ops.mesh.primitive_torus_add(major_radius=1,minor_radius=0.48,major_segments=64,minor_segments=24,location=loc)
    f=bpy.context.object; f.name=name+'_frost'; f.data.materials.append(mat('frost'+name,FROST,0.25))
    bpy.ops.object.shade_smooth()
    bpy.ops.object.mode_set(mode='EDIT'); bpy.ops.mesh.select_all(action='DESELECT'); bpy.ops.object.mode_set(mode='OBJECT')
    for v in f.data.vertices: v.select = v.co.z < 0.05
    bpy.ops.object.mode_set(mode='EDIT'); bpy.ops.mesh.delete(type='VERT'); bpy.ops.object.mode_set(mode='OBJECT')
    f.parent=d; f.location=(0,0,0)
    r=random.Random(seed); sm=[mat(f'spr{i}{name}',c,0.4) for i,c in enumerate(SPR)]
    for i in range(sprinkles):
        a=r.random()*math.tau; rr=1+r.uniform(-0.3,0.3); z=0.47*math.sqrt(max(0,1-((rr-1)/0.45)**2))
        bpy.ops.mesh.primitive_cylinder_add(radius=0.03,depth=0.16,location=(rr*math.cos(a),rr*math.sin(a),z+0.02))
        sp=bpy.context.object; sp.rotation_euler=(math.pi/2,0,r.random()*math.pi); sp.data.materials.append(sm[i%len(sm)]); sp.parent=d
    d.scale=(scale,scale,scale)
    return d

def sun(strength=4,rot=(0.8,0.2,0.6)):
    bpy.ops.object.light_add(type='SUN',rotation=rot); l=bpy.context.object; l.data.energy=strength; l.data.angle=0.1; return l

def camera(loc,target):
    bpy.ops.object.camera_add(location=loc); c=bpy.context.object
    bpy.ops.object.empty_add(location=target); t=bpy.context.object
    con=c.constraints.new('TRACK_TO'); con.target=t; con.track_axis='TRACK_NEGATIVE_Z'; con.up_axis='UP_Y'
    bpy.context.scene.camera=c; c.data.lens=40; return c,t

def key(o,frame,**kw):
    for k,v in kw.items(): setattr(o,k,v); o.keyframe_insert(data_path=k,frame=frame)
