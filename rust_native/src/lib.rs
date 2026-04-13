use rayon::prelude::*;
use std::slice;
use std::f64;

#[repr(C)]
pub struct NoiseData {
    pub x: f64,
    pub y: f64,
    pub z: f64,
    pub result: f64,
}

// Simple fast hash for noise permutations
fn p(hash: &[u8; 512], x: i32) -> u8 {
    hash[(x & 255) as usize]
}

// Grad dot product
fn grad_dot(hash: i32, x: f64, y: f64, z: f64) -> f64 {
    let h = hash & 15;
    let u = if h < 8 { x } else { y };
    let v = if h < 4 { y } else if h == 12 || h == 14 { x } else { z };
    (if (h & 1) != 0 { -u } else { u }) + (if (h & 2) != 0 { -v } else { v })
}

fn curver(t: f64) -> f64 {
    t * t * t * (t * (t * 6.0 - 15.0) + 10.0)
}

fn lerp(t: f64, a: f64, b: f64) -> f64 {
    a + t * (b - a)
}

#[no_mangle]
pub extern "C" fn calculate_terrain_noise_bulk(
    ptr: *mut NoiseData,
    length: i32,
    p_array_ptr: *const u8,
) {
    if ptr.is_null() || length <= 0 || p_array_ptr.is_null() {
        return;
    }

    let noise_data_slice = unsafe { slice::from_raw_parts_mut(ptr, length as usize) };
    let mut hash = [0u8; 512];
    let p_slice = unsafe { slice::from_raw_parts(p_array_ptr, 256) };
    for i in 0..256 {
        hash[i] = p_slice[i];
        hash[i + 256] = p_slice[i];
    }

    // Rayon parallel iterator!
    noise_data_slice.par_iter_mut().for_each(|data| {
        let x = data.x;
        let y = data.y;
        let z = data.z;

        let floor_x = x.floor() as i32;
        let floor_y = y.floor() as i32;
        let floor_z = z.floor() as i32;

        let frac_x = x - floor_x as f64;
        let frac_y = y - floor_y as f64;
        let frac_z = z - floor_z as f64;

        let u = curver(frac_x);
        let v = curver(frac_y);
        let w = curver(frac_z);

        let a = p(&hash, floor_x) as i32;
        let aa = p(&hash, a + floor_y) as i32;
        let ab = p(&hash, a + floor_y + 1) as i32;
        let b = p(&hash, floor_x + 1) as i32;
        let ba = p(&hash, b + floor_y) as i32;
        let bb = p(&hash, b + floor_y + 1) as i32;

        let lerp1 = lerp(u, grad_dot(p(&hash, aa + floor_z) as i32, frac_x, frac_y, frac_z),
                     grad_dot(p(&hash, ba + floor_z) as i32, frac_x - 1.0, frac_y, frac_z));
        let lerp2 = lerp(u, grad_dot(p(&hash, ab + floor_z) as i32, frac_x, frac_y - 1.0, frac_z),
                     grad_dot(p(&hash, bb + floor_z) as i32, frac_x - 1.0, frac_y - 1.0, frac_z));
        let lerp3 = lerp(u, grad_dot(p(&hash, aa + floor_z + 1) as i32, frac_x, frac_y, frac_z - 1.0),
                     grad_dot(p(&hash, ba + floor_z + 1) as i32, frac_x - 1.0, frac_y, frac_z - 1.0));
        let lerp4 = lerp(u, grad_dot(p(&hash, ab + floor_z + 1) as i32, frac_x, frac_y - 1.0, frac_z - 1.0),
                     grad_dot(p(&hash, bb + floor_z + 1) as i32, frac_x - 1.0, frac_y - 1.0, frac_z - 1.0));

        let res = lerp(w, lerp(v, lerp1, lerp2), lerp(v, lerp3, lerp4));
        data.result = res;
    });
}

#[no_mangle]
pub extern "C" fn calculate_terrain_noise_single(
    x: f64, y: f64, z: f64,
    p_array_ptr: *const u8,
) -> f64 {
    if p_array_ptr.is_null() {
        return 0.0;
    }
    
    let mut hash = [0u8; 512];
    let p_slice = unsafe { slice::from_raw_parts(p_array_ptr, 256) };
    for i in 0..256 {
        hash[i] = p_slice[i];
        hash[i + 256] = p_slice[i];
    }
    
    let floor_x = x.floor() as i32;
    let floor_y = y.floor() as i32;
    let floor_z = z.floor() as i32;

    let frac_x = x - floor_x as f64;
    let frac_y = y - floor_y as f64;
    let frac_z = z - floor_z as f64;

    let u = curver(frac_x);
    let v = curver(frac_y);
    let w = curver(frac_z);

    let a = p(&hash, floor_x) as i32;
    let aa = p(&hash, a + floor_y) as i32;
    let ab = p(&hash, a + floor_y + 1) as i32;
    let b = p(&hash, floor_x + 1) as i32;
    let ba = p(&hash, b + floor_y) as i32;
    let bb = p(&hash, b + floor_y + 1) as i32;

    let lerp1 = lerp(u, grad_dot(p(&hash, aa + floor_z) as i32, frac_x, frac_y, frac_z),
                 grad_dot(p(&hash, ba + floor_z) as i32, frac_x - 1.0, frac_y, frac_z));
    let lerp2 = lerp(u, grad_dot(p(&hash, ab + floor_z) as i32, frac_x, frac_y - 1.0, frac_z),
                 grad_dot(p(&hash, bb + floor_z) as i32, frac_x - 1.0, frac_y - 1.0, frac_z));
    let lerp3 = lerp(u, grad_dot(p(&hash, aa + floor_z + 1) as i32, frac_x, frac_y, frac_z - 1.0),
                 grad_dot(p(&hash, ba + floor_z + 1) as i32, frac_x - 1.0, frac_y, frac_z - 1.0));
    let lerp4 = lerp(u, grad_dot(p(&hash, ab + floor_z + 1) as i32, frac_x, frac_y - 1.0, frac_z - 1.0),
                 grad_dot(p(&hash, bb + floor_z + 1) as i32, frac_x - 1.0, frac_y - 1.0, frac_z - 1.0));

    lerp(w, lerp(v, lerp1, lerp2), lerp(v, lerp3, lerp4))
}
