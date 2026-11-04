// Runs beside Horizon's renderer so the attachment uses exactly the same
// vanilla/baby/CEM tree and animation evaluation as the visible body.
use super::{Mob, MobData, Part, Poses, posed, posed_body};
use glam::{Mat4, Vec3};

pub struct Torso {
    pub matrix: Mat4,
    pub min: Vec3,
    pub max: Vec3,
}

fn limb(name: &str) -> bool {
    let name = name.to_ascii_lowercase();
    ["head", "neck", "mane", "leg", "paw", "tail", "ear"]
        .iter()
        .any(|s| name.contains(s))
}

fn measure(part: &Part, parent: Mat4, poses: &Poses, min: &mut Vec3, max: &mut Vec3) {
    if poses.is_hidden(part) || limb(&part.name) {
        return;
    }
    let matrix = posed(parent, poses.pose(part));
    if !poses.skips(part) {
        for polygon in &part.polygons {
            for vertex in polygon.vertices {
                let vertex = matrix.transform_point3(Vec3::new(vertex[0], vertex[1], vertex[2]));
                *min = min.min(vertex);
                *max = max.max(vertex);
            }
        }
    }
    for child in &part.children {
        measure(child, matrix, poses, min, max);
    }
}

fn find<'a>(part: &'a Part, parent: Mat4, poses: &Poses) -> Option<(&'a Part, Mat4)> {
    if poses.is_hidden(part) {
        return None;
    }
    let matrix = posed(parent, poses.pose(part));
    if part.name == "body" {
        return Some((part, matrix));
    }
    part.children
        .iter()
        .find_map(|child| find(child, matrix, poses))
}

pub fn torso(mob: &Mob, partial: f32, data: &MobData, base: Mat4) -> Option<Torso> {
    let (model, poses, _) = posed_body(mob, partial, data);
    let (part, matrix) = find(&model, base, &poses)?;
    let mut min = Vec3::splat(f32::INFINITY);
    let mut max = Vec3::splat(f32::NEG_INFINITY);
    if !poses.skips(part) {
        for polygon in &part.polygons {
            for vertex in polygon.vertices {
                let vertex = Vec3::new(vertex[0], vertex[1], vertex[2]);
                min = min.min(vertex);
                max = max.max(vertex);
            }
        }
    }
    for child in &part.children {
        measure(child, Mat4::IDENTITY, &poses, &mut min, &mut max);
    }
    if !min.is_finite() || !max.is_finite() || (max - min).min_element() <= 0.0 {
        return None;
    }
    Some(Torso { matrix, min, max })
}
