__kernel void fft(const __global float2 * g_data, __local float2 * l_data, uint size, int dir) {

    int workitems_per_workgroup = get_local_size(0);
    int number_of_workgroups = get_num_groups(0);
    int points_per_group = size/number_of_workgroups;
    int points_per_item = points_per_group /  get_local_size(0);
    int l_addr = get_local_id(0) * points_per_item;
    int g_addr = get_group_id(0) * points_per_group + l_addr;

    uint4 index, br;
    uint mask_left, mask_right, shift_pos;
    float2 x1, x2, x3, x4;

    for(int i = 0; i < points_per_item; i += 4) {
        index = (uint4) (g_addr, g_addr + 1, g_addr + 2, g_addr + 3);
        mask_left = size / 2;
        mask_right = 1;
        shift_pos = (int)log2((float) size) - 1;

        br = (index << shift_pos) & mask_left;
        br |= (index >> shift_pos) & mask_right;

        while(shift_pos > 1) {
            shift_pos -= 2;
            mask_left >>= 1;
            mask_right <<= 1;
            br |= (index << shift_pos) & mask_left;
            br |= (index >> shift_pos) & mask_right;
        }

        x1 = g_data[br.s0];
        x2 = g_data[br.s1];
        x3 = g_data[br.s2];
        x4 = g_data[br.s3];

        float2 sum12 = x1 + x2;
        float2 diff12 = x1 - x2;
        float2 sum34 = x3 + x4;
        float2 diff34 = (float2) (x3.s1 - x4.s1, x4.s0 - x3.s0) * dir;

        l_data[l_addr] = sum12 + sum34;
        l_data[l_addr+1] = diff12 + diff34;
        l_data[l_addr+2] = sum12 + sum34;
        l_data[l_addr+3] = diff12 + diff34;
        l_addr += 4;
        g_addr += 4;
    }

    float cosine, sine;
    float2 wk;

    for(int N2 = 4; N2 < points_per_item; N2 <<=1) {
        l_addr = get_local_id(0) * points_per_item;
        for(int fft_index = 0; fft_index < points_per_item; fft_index += 2*N2) {
            x1 = l_data[l_addr];
            l_data[l_addr] += l_data[l_addr + N2];
            l_data[l_addr + N2] = x1 - l_data[l_addr + N2];

            for(int i = 1; i < N2; i++) {
                float param = (M_PI_F * i)/N2;
                sine = sincos(param,&cosine);
                wk = (float2) (
                    l_data[l_addr + N2 + i].s0 * cosine +
                    l_data[l_addr + N2 + i].s1 * sine,
                    l_data[l_addr + N2 + i].s1 * cosine -
                    l_data[l_addr + N2 + i].s0 * sine);
                l_data[l_addr + N2 + i] = l_data[l_addr + i] - wk;
                l_data[l_addr + i] += wk;
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
            float param = M_PI_F * angle/N2;
            sine = sincos(param,&cosine);
            sine = direction*sine;

            wk = (float2) (
                l_data[N2 + i].s0 * cosine +
                l_data[N2 + i].s1 * sine,
                l_data[N2 + i].s1 * cosine -
                l_data[N2 + i].s0 * sine);
            l_data[N2 + i] = l_data[i] - wk;
            l_data[i] += wk;
            angle++;
        }
        stage <<= 1;
        barrier(CLK_LOCAL_MEM_FENCE);
    }

    // write back into input
    barrier(CLK_GLOBAL_MEM_FENCE);

    l_addr = get_local_id(0)*points_per_item; // address within one workitem
    g_addr = get_group_id(0)*points_per_group + l_addr;

    float factor = (1/(float)N);
    for (int i = 0; i < points_per_item; ++i) {
        int index = g_addr+i;

        if (direction == 1) {
            g_data[index] = l_data[l_addr+i];
        } else { // inverse FFT
            float2 val = l_data[l_addr+i];
            g_data[index] = (float2)(factor*val.x,factor*val.y);
        }
    }

}