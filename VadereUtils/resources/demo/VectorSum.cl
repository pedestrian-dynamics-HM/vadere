kernel void add(__global float2* a, __global float2* b, __global float2* c) {
    int i = get_global_id(0);
    c[i] = a[i] * b[i];
}