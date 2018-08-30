__kernel void fft2Dim(const __global float2 *input,
                      __local float2 *local_data,
                      __global float2 *output,
                      const uint M, const uint N,
                      const int direction) {

    int length = M*N; // size of matrix as vector

    if (get_global_id(0) >= length)
        return;

    int workitems_per_workgroup = get_local_size(0); // number of work items in workgroup
    int number_of_workgroups = get_num_groups(0);

    int points_per_group = length/number_of_workgroups; // points per group
    int points_per_item = points_per_group/workitems_per_workgroup; // points in one workitem

    int l_addr = get_local_id(0)*points_per_item; // address within one workitem
    int g_addr = get_group_id(0)*points_per_group + l_addr; // address within the hole workgroup

    int workitems_per_row = N/points_per_item; // number of workitems div by number of rows
    int reindexing_offset = get_global_id(0)/workitems_per_row;

    uint4 index, br;
    uint mask_left, mask_right, shift_pos;
    float2 x1, x2, x3, x4;

    for(int i = 0; i < points_per_item; i += 4) {
        index = (uint4) (g_addr, g_addr + 1, g_addr + 2, g_addr + 3);
        mask_left = N / 2;
        mask_right = 1;
        shift_pos = (int)log2((float)N) - 1;

        br = (index << shift_pos) & mask_left;
        br |= (index >> shift_pos) & mask_right;

        while(shift_pos > 1) {
            shift_pos -= 2;
            mask_left >>= 1;
            mask_right <<= 1;
            br |= (index << shift_pos) & mask_left;
            br |= (index >> shift_pos) & mask_right;
        }

        x1 = input[br.s0+reindexing_offset*N];
        x2 = input[br.s1+reindexing_offset*N];
        x3 = input[br.s2+reindexing_offset*N];
        x4 = input[br.s3+reindexing_offset*N];

        float2 sum12 = x1 + x2;
        float2 diff12 = x1 - x2;
        float2 sum34 = x3 + x4;
        float2 diff34 = x3 - x4; //(float2) (x3.y - x4.y, x4.x - x3.x)*direction; // turned aroung because of w3 (0,-i)

        local_data[l_addr] = sum12 + sum34;
        local_data[l_addr+1] = diff12 + (float2)(diff34.y,-diff34.x)*direction;
        local_data[l_addr+2] = sum12 - sum34;
        local_data[l_addr+3] =  diff12 + (float2)(-diff34.y,diff34.x)*direction;

        g_addr += 4;
        l_addr += 4;
    }


    float cosine, sine;
    float2 wk;

    for(int N2 = 4; N2 < points_per_item; N2 <<=1) {
        l_addr = get_local_id(0) * points_per_item;

        for(int fft_index = 0; fft_index < points_per_item; fft_index += 2*N2) {
            x1 = local_data[l_addr];
            local_data[l_addr] += local_data[l_addr + N2];
            local_data[l_addr + N2] = x1 - local_data[l_addr + N2];

            for(int i = 1; i < N2; i++) {
                cosine = cos(M_PI_F * i /N2);
                sine = direction*sin(M_PI_F*i/N2);

                wk = (float2) (
                    local_data[l_addr + N2 + i].s0 * cosine +
                    local_data[l_addr + N2 + i].s1 * sine,
                    local_data[l_addr + N2 + i].s1 * cosine -
                    local_data[l_addr + N2 + i].s0 * sine);
                local_data[l_addr + N2 + i] = local_data[l_addr + i] - wk;
                local_data[l_addr + i] += wk;
            }
            l_addr += 2*N2;
        }
    }
    barrier(CLK_LOCAL_MEM_FENCE);

    uint start, angle, stage;
    stage = 2;


    for(int N2 = points_per_item; N2 < N; N2 <<=1) {
        start = (get_local_id(0) + (get_local_id(0)/stage)*stage) * (points_per_item/2);
        angle = start % (N2*2);

        for(int i = start; i < start + points_per_item / 2; i++) {
            cosine = cos(M_PI_F * angle /N2);
            sine = direction*sin(M_PI_F*angle/N2);

            wk = (float2) (
                local_data[N2 + i].s0 * cosine +
                local_data[N2 + i].s1 * sine,
                local_data[N2 + i].s1 * cosine -
                local_data[N2 + i].s0 * sine);
            local_data[N2 + i] = local_data[i] - wk;
            local_data[i] += wk;

            angle++;
        }

        stage <<= 1;
        barrier(CLK_LOCAL_MEM_FENCE);
    }

    // copy data back into global memory and transform
    barrier(CLK_GLOBAL_MEM_FENCE);

    l_addr = get_local_id(0)*points_per_item; // address within one workitem
    g_addr = get_group_id(0)*points_per_group + l_addr;

    float factor = (1/(float)N);
    for (int i = 0; i < points_per_item; ++i) {

        int index = g_addr+i;
        int row = index/N;
        int col = index%N;
        int indexTransposed = M*col + row;

        if (direction == 1) {
            output[indexTransposed] = local_data[l_addr+i];
        } else { // inverse FFT
            float2 val = local_data[l_addr+i];
            output[indexTransposed] = (float2)(factor*val.x,factor*val.y);
        }
    }
}

// Kernel for fft convolution row/col

__kernel void multiply(const __global float2 *paddedMatrix, const __global float2 *paddedKernel, __global float2 *output,
    const uint height, const uint width) {

    int i = get_global_id(0); // row
    int j = get_global_id(1); // col

    int index = i*width + j;
    int indexTransposed = j*height + i;
    float2 m = paddedMatrix[index];
    float2 k = paddedKernel[j];
    output[indexTransposed] = (float2) (m.x * k.x - m.y * k.y, m.x * k.y + m.y*k.x);
}

// 1 dimensional FFT
// __global float2 *output,
// const uint points_per_group,

__kernel void fft1Dim(__global float2 *input,
                      __local float2 *local_data,
                      const uint N,
                      const int direction) {

    int g_id = get_global_id(0); // Returns the element of the work-item’s global ID for a given dimension
    if (g_id >= N)
        return;

    int workitems_per_workgroup = get_local_size(0); // number of work items in workgroup
    int number_of_workgroups = get_num_groups(0);
    int points_per_group = N/number_of_workgroups; // points per group

    int points_per_item = points_per_group/workitems_per_workgroup; // points in one workitem
    int l_addr = get_local_id(0)*points_per_item; // address within one workitem
    int global_addr = get_group_id(0)*points_per_group + l_addr; // address within the hole workgroup

    uint4 index, br;
    uint mask_left, mask_right, shift_pos;
    float2 x1, x2, x3, x4;

    for(int i = 0; i < points_per_item; i += 4) {
        index = (uint4) (global_addr, global_addr + 1, global_addr + 2, global_addr + 3);
        mask_left = N / 2;
        mask_right = 1;
        shift_pos = (int)log2((float)N) - 1;

        br = (index << shift_pos) & mask_left;
        br |= (index >> shift_pos) & mask_right;

        while(shift_pos > 1) {
            shift_pos -= 2;
            mask_left >>= 1;
            mask_right <<= 1;
            br |= (index << shift_pos) & mask_left;
            br |= (index >> shift_pos) & mask_right;
        }

        x1 = input[br.s0];
        x2 = input[br.s1];
        x3 = input[br.s2];
        x4 = input[br.s3];

        float2 sum12 = x1 + x2;
        float2 diff12 = x1 - x2;
        float2 sum34 = x3 + x4;
        float2 diff34 = x3 - x4; //(float2) (x3.y - x4.y, x4.x - x3.x)*direction; // turned aroung because of w3 (0,-i)

        local_data[l_addr] = sum12 + sum34;
        local_data[l_addr+1] = diff12 + (float2)(diff34.y,-diff34.x)*direction;
        local_data[l_addr+2] = sum12 - sum34;
        local_data[l_addr+3] =  diff12 + (float2)(-diff34.y,diff34.x)*direction;

        global_addr += 4;
        l_addr += 4;
    }


    float cosine, sine;
    float2 wk;

    for(int N2 = 4; N2 < points_per_item; N2 <<=1) {
        l_addr = get_local_id(0) * points_per_item;

        for(int fft_index = 0; fft_index < points_per_item; fft_index += 2*N2) {
            x1 = local_data[l_addr];
            local_data[l_addr] += local_data[l_addr + N2];
            local_data[l_addr + N2] = x1 - local_data[l_addr + N2];

            for(int i = 1; i < N2; i++) {
                cosine = cos(M_PI_F * i /N2);
                sine = direction*sin(M_PI_F*i/N2);

                wk = (float2) (
                    local_data[l_addr + N2 + i].s0 * cosine +
                    local_data[l_addr + N2 + i].s1 * sine,
                    local_data[l_addr + N2 + i].s1 * cosine -
                    local_data[l_addr + N2 + i].s0 * sine);

                local_data[l_addr + N2 + i] = local_data[l_addr + i] - wk;
                local_data[l_addr + i] += wk;

            }
            l_addr += 2*N2;
        }
    }
    barrier(CLK_LOCAL_MEM_FENCE);

    uint start, angle, stage;
    stage = 2;

    for(int N2 = points_per_item; N2 < points_per_group; N2 <<=1) {
        start = (get_local_id(0) + (get_local_id(0)/stage)*stage) * (points_per_item/2);
        angle = start % (N2*2);

        for(int i = start; i < start + points_per_item / 2; i++) {
            cosine = cos(M_PI_F * angle /N2);
            sine = direction*sin(M_PI_F*angle/N2);

            wk = (float2) (
                local_data[N2 + i].s0 * cosine +
                local_data[N2 + i].s1 * sine,
                local_data[N2 + i].s1 * cosine -
                local_data[N2 + i].s0 * sine);
            local_data[N2 + i] = local_data[i] - wk;
            local_data[i] += wk;

            angle++;
        }

        stage <<= 1;
        barrier(CLK_LOCAL_MEM_FENCE);
    }


    // data put back into global memory

    l_addr = get_local_id(0)*points_per_item; // address within one workitem
    global_addr = get_group_id(0)*points_per_group + l_addr; // address within the hole workgroup

    for (int i = 0; i < points_per_item; ++i) {
        if (direction > 0) {
            input[global_addr+i] = local_data[l_addr+i];
        } else {
            float2 value = local_data[l_addr+i];
            float factor = (1/(float)N);
            input[global_addr+i] = (float2)(factor*value.x,factor*value.y);
        }
    }

}