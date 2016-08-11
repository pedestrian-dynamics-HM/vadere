//KERNEL_SIMPLE with crop strategy
__kernel void convolve(const __global  float * pInput,
                        __constant float * pFilter,
                        __global  float * pOutput,
                        const int nInWidth,
                        const int nInHeight,
                        const int nFilterWidth)
{
    int nWidth = get_global_size(0);

    int xOut = get_global_id(0);
    int yOut = get_global_id(1);

    int xInTopLeft = xOut;
    int yInTopLeft = yOut;

    int bottomBorder = (nFilterWidth / 2 + 1) - (nInWidth - yOut);
    bottomBorder = bottomBorder > 0 ? bottomBorder : 0;
    int topBorder = yOut - (nFilterWidth / 2) < 0 ? yOut - (nFilterWidth / 2) : 0;

    int rightBorder = (nFilterWidth / 2 + 1) - (nInHeight - xOut);
    rightBorder = rightBorder > 0 ? rightBorder : 0;
    int leftBorder = xOut - (nFilterWidth / 2) < 0 ? xOut - (nFilterWidth / 2) : 0;

    float sum = 0;
    for (int r = -nFilterWidth / 2 - topBorder; r <= nFilterWidth / 2 - bottomBorder; r++)
    {
        int idxFtmp = (r + nFilterWidth / 2) * nFilterWidth;

        int yIn = yInTopLeft + r;
        int idxIntmp = yIn * nInWidth + xInTopLeft;

        for (int c = - nFilterWidth / 2 - leftBorder; c <= nFilterWidth / 2 - rightBorder; c++)
        {
            int idxF  = idxFtmp  + (c + nFilterWidth / 2);
            int idxIn = idxIntmp + c;
            sum += pFilter[idxF]*pInput[idxIn];
        }
    }
    int idxOut = yOut * nWidth + xOut;
    pOutput[idxOut] = sum;
}
//KERNEL_SIMPLE


__kernel void convolveRow(const __global  float * pInput,
                        __constant float * pFilter,
                        __global  float * pOutput,
                        const int nInWidth,
                        const int nInHeight,
                        const int nFilterWidth)
{
    int nWidth = get_global_size(0);

    int xOut = get_global_id(0);
    int yOut = get_global_id(1);

    int xInTopLeft = xOut;
    int yInTopLeft = yOut;

    int bottomBorder = (nFilterWidth / 2 + 1) - (nInHeight - yOut);
    bottomBorder = bottomBorder > 0 ? bottomBorder : 0;
    int topBorder = yOut - (nFilterWidth / 2) < 0 ? yOut - (nFilterWidth / 2) : 0;

    float sum = 0;
    for (int r = -nFilterWidth / 2 - topBorder; r <= nFilterWidth / 2 - bottomBorder; r++)
    {
        int idxF = (r + nFilterWidth / 2);
        int yIn = yInTopLeft * nInWidth;
        int idxIn = yIn + xInTopLeft + r * nInWidth;
        sum += pFilter[idxF] * pInput[idxIn];
    }
    int idxOut = yOut * nWidth + xOut;
    pOutput[idxOut] = sum;
}

__kernel void convolveCol(const __global  float * pInput,
                        __constant float * pFilter,
                        __global  float * pOutput,
                        const int nInWidth,
                        const int nInHeight,
                        const int nFilterWidth)
{
    int nWidth = get_global_size(0);

    int xOut = get_global_id(0);
    int yOut = get_global_id(1);

    int xInTopLeft = xOut;
    int yInTopLeft = yOut;

    int rightBorder = (nFilterWidth / 2 + 1) - (nInWidth - xOut);
    rightBorder = rightBorder > 0 ? rightBorder : 0;
    int leftBorder = xOut - (nFilterWidth / 2) < 0 ? xOut - (nFilterWidth / 2) : 0;


    float sum = 0;
    for (int r = -nFilterWidth / 2 - leftBorder; r <= nFilterWidth / 2 - rightBorder; r++)
    {
        int idxF = (r + nFilterWidth / 2);
        int yIn = yInTopLeft * nInWidth;
        int idxIn = yIn + xInTopLeft + r;
        sum += pFilter[idxF] * pInput[idxIn];
    }
    int idxOut = yOut * nWidth + xOut;
    pOutput[idxOut] = sum;
}