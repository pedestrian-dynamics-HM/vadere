#pragma OPENCL EXTENSION cl_khr_fp64 : enable

__kernel void dft1Dim(const __global float2 * input, __global float2 * output,
                    const int length, const int direction)
{
    int N = length; // only halft the length because of float2 vector
    int i = get_global_id(0);

    if (i >= N)
        return;

    float2 sum = (float2)(0.0f,0.0f);
    float param = (-2*direction * i) * M_PI_F / (float) N;

    for (int k = 0; k < N; ++k) {
            float2 value = input[k];

            float c;
            float s = sincos(k* param, &c);
            sum += (float2)(value.x * c - value.y*s,value.x * s + value.y*c);
    }

    if (direction == 1) {
        output[i] = sum;
    } else {
        output[i] = (float2)(sum.x/(float)N,sum.y/(float)N);
    }
}