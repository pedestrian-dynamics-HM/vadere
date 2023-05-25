package org.vadere.util.opencl;

import static org.lwjgl.opencl.CL10.CL_COMPLETE;
import static org.lwjgl.opencl.CL10.CL_CONTEXT_PLATFORM;
import static org.lwjgl.opencl.CL10.CL_DEVICE_ADDRESS_BITS;
import static org.lwjgl.opencl.CL10.CL_DEVICE_AVAILABLE;
import static org.lwjgl.opencl.CL10.CL_DEVICE_COMPILER_AVAILABLE;
import static org.lwjgl.opencl.CL10.CL_DEVICE_EXECUTION_CAPABILITIES;
import static org.lwjgl.opencl.CL10.CL_DEVICE_EXTENSIONS;
import static org.lwjgl.opencl.CL10.CL_DEVICE_MAX_CLOCK_FREQUENCY;
import static org.lwjgl.opencl.CL10.CL_DEVICE_MAX_COMPUTE_UNITS;
import static org.lwjgl.opencl.CL10.CL_DEVICE_MAX_WORK_GROUP_SIZE;
import static org.lwjgl.opencl.CL10.CL_DEVICE_MAX_WORK_ITEM_DIMENSIONS;
import static org.lwjgl.opencl.CL10.CL_DEVICE_NAME;
import static org.lwjgl.opencl.CL10.CL_DEVICE_PROFILE;
import static org.lwjgl.opencl.CL10.CL_DEVICE_TYPE;
import static org.lwjgl.opencl.CL10.CL_DEVICE_TYPE_ALL;
import static org.lwjgl.opencl.CL10.CL_DEVICE_VENDOR;
import static org.lwjgl.opencl.CL10.CL_DEVICE_VENDOR_ID;
import static org.lwjgl.opencl.CL10.CL_DEVICE_VERSION;
import static org.lwjgl.opencl.CL10.CL_DRIVER_VERSION;
import static org.lwjgl.opencl.CL10.CL_EXEC_NATIVE_KERNEL;
import static org.lwjgl.opencl.CL10.CL_MEM_READ_ONLY;
import static org.lwjgl.opencl.CL10.CL_PLATFORM_EXTENSIONS;
import static org.lwjgl.opencl.CL10.CL_PLATFORM_NAME;
import static org.lwjgl.opencl.CL10.CL_PLATFORM_PROFILE;
import static org.lwjgl.opencl.CL10.CL_PLATFORM_VENDOR;
import static org.lwjgl.opencl.CL10.CL_PLATFORM_VERSION;
import static org.lwjgl.opencl.CL10.CL_QUEUED;
import static org.lwjgl.opencl.CL10.CL_RUNNING;
import static org.lwjgl.opencl.CL10.CL_SUBMITTED;
import static org.lwjgl.opencl.CL10.clCreateBuffer;
import static org.lwjgl.opencl.CL10.clCreateCommandQueue;
import static org.lwjgl.opencl.CL10.clCreateContext;
import static org.lwjgl.opencl.CL10.clEnqueueNativeKernel;
import static org.lwjgl.opencl.CL10.clFinish;
import static org.lwjgl.opencl.CL10.clGetDeviceIDs;
import static org.lwjgl.opencl.CL10.clGetPlatformIDs;
import static org.lwjgl.opencl.CL10.clReleaseCommandQueue;
import static org.lwjgl.opencl.CL10.clReleaseContext;
import static org.lwjgl.opencl.CL10.clReleaseEvent;
import static org.lwjgl.opencl.CL10.clReleaseMemObject;
import static org.lwjgl.opencl.CL11.CL_BUFFER_CREATE_TYPE_REGION;
import static org.lwjgl.opencl.CL11.CL_DEVICE_NATIVE_VECTOR_WIDTH_DOUBLE;
import static org.lwjgl.opencl.CL11.CL_DEVICE_OPENCL_C_VERSION;
import static org.lwjgl.opencl.CL11.clSetEventCallback;
import static org.lwjgl.opencl.CL11.clSetMemObjectDestructorCallback;
import static org.lwjgl.opencl.CL11.nclCreateSubBuffer;
import static org.lwjgl.opencl.KHRICD.CL_PLATFORM_ICD_SUFFIX_KHR;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.system.MemoryUtil.memAddress;
import static org.lwjgl.system.MemoryUtil.memByteBuffer;
import static org.lwjgl.system.MemoryUtil.memUTF8;
import static org.lwjgl.system.Pointer.POINTER_SIZE;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opencl.CL;
import org.lwjgl.opencl.CLBufferRegion;
import org.lwjgl.opencl.CLCapabilities;
import org.lwjgl.opencl.CLContextCallback;
import org.lwjgl.opencl.CLEventCallback;
import org.lwjgl.opencl.CLMemObjectDestructorCallback;
import org.lwjgl.opencl.CLNativeKernel;
import org.lwjgl.system.FunctionProviderLocal;
import org.lwjgl.system.MemoryStack;
import org.vadere.util.logging.Logger;

/*
 * Copyright LWJGL. All rights reserved.
 * License terms: https://www.lwjgl.org/license
 */
public final class CLDemo {

  public static Logger logger = Logger.getLogger(CLDemo.class);

  private CLDemo() {}

  public static void main(String[] args) {
    try (MemoryStack stack = stackPush()) {
      demo(stack);
    } catch (OpenCLException e) {
      e.printStackTrace();
    }
  }

  private static void demo(MemoryStack stack) throws OpenCLException {
    IntBuffer pi = stack.mallocInt(1);
    try {
      CLInfo.checkCLError(clGetPlatformIDs(null, pi));
      if (pi.get(0) == 0) {
        throw new RuntimeException("No OpenCL platforms found.");
      }

      PointerBuffer platforms = stack.mallocPointer(pi.get(0));
      CLInfo.checkCLError(clGetPlatformIDs(platforms, (IntBuffer) null));

      PointerBuffer ctxProps = stack.mallocPointer(3);
      ctxProps.put(0, CL_CONTEXT_PLATFORM).put(2, 0);

      IntBuffer errcode_ret = stack.callocInt(1);
      for (int p = 0; p < platforms.capacity(); p++) {
        long platform = platforms.get(p);
        ctxProps.put(1, platform);

        System.out.println("\n-------------------------");
        System.out.printf("NEW PLATFORM: [0x%X]\n", platform);

        CLCapabilities platformCaps = CL.createPlatformCapabilities(platform);

        printPlatformInfo(platform, "CL_PLATFORM_PROFILE", CL_PLATFORM_PROFILE);
        printPlatformInfo(platform, "CL_PLATFORM_VERSION", CL_PLATFORM_VERSION);
        printPlatformInfo(platform, "CL_PLATFORM_NAME", CL_PLATFORM_NAME);
        printPlatformInfo(platform, "CL_PLATFORM_VENDOR", CL_PLATFORM_VENDOR);
        printPlatformInfo(platform, "CL_PLATFORM_EXTENSIONS", CL_PLATFORM_EXTENSIONS);
        if (platformCaps.cl_khr_icd) {
          printPlatformInfo(platform, "CL_PLATFORM_ICD_SUFFIX_KHR", CL_PLATFORM_ICD_SUFFIX_KHR);
        }
        System.out.println("");

        CLInfo.checkCLError(clGetDeviceIDs(platform, CL_DEVICE_TYPE_ALL, null, pi));

        PointerBuffer devices = stack.mallocPointer(pi.get(0));
        CLInfo.checkCLError(
            clGetDeviceIDs(platform, CL_DEVICE_TYPE_ALL, devices, (IntBuffer) null));

        for (int d = 0; d < devices.capacity(); d++) {
          long device = devices.get(d);

          CLCapabilities caps = CL.createDeviceCapabilities(device, platformCaps);

          System.out.printf("\n\t** NEW DEVICE: [0x%X]\n", device);
          System.out.println(
              "\tCL_DEVICE_TYPE = " + CLInfo.getDeviceInfoLong(device, CL_DEVICE_TYPE));
          System.out.println(
              "\tCL_DEVICE_VENDOR_ID = " + CLInfo.getDeviceInfoInt(device, CL_DEVICE_VENDOR_ID));
          System.out.println(
              "\tCL_DEVICE_MAX_COMPUTE_UNITS = "
                  + CLInfo.getDeviceInfoInt(device, CL_DEVICE_MAX_COMPUTE_UNITS));
          System.out.println(
              "\tCL_DEVICE_NATIVE_VECTOR_WIDTH_DOUBLE = "
                  + CLInfo.getDeviceInfoLong(device, CL_DEVICE_NATIVE_VECTOR_WIDTH_DOUBLE));
          // System.out.println("\tCL_KERNEL_PREFERRED_WORK_GROUP_SIZE_MULTIPLE = " +
          // InfoUtils.getDeviceInfoPointer(device, CL_KERNEL_PREFERRED_WORK_GROUP_SIZE_MULTIPLE));
          System.out.println(
              "\tCL_DEVICE_MAX_WORK_ITEM_DIMENSIONS = "
                  + CLInfo.getDeviceInfoInt(device, CL_DEVICE_MAX_WORK_ITEM_DIMENSIONS));
          System.out.println(
              "\tCL_DEVICE_MAX_WORK_GROUP_SIZE = "
                  + CLInfo.getDeviceInfoPointer(device, CL_DEVICE_MAX_WORK_GROUP_SIZE));
          System.out.println(
              "\tCL_DEVICE_MAX_CLOCK_FREQUENCY = "
                  + CLInfo.getDeviceInfoInt(device, CL_DEVICE_MAX_CLOCK_FREQUENCY));
          System.out.println(
              "\tCL_DEVICE_ADDRESS_BITS = "
                  + CLInfo.getDeviceInfoInt(device, CL_DEVICE_ADDRESS_BITS));
          System.out.println(
              "\tCL_DEVICE_AVAILABLE = "
                  + (CLInfo.getDeviceInfoInt(device, CL_DEVICE_AVAILABLE) != 0));
          System.out.println(
              "\tCL_DEVICE_COMPILER_AVAILABLE = "
                  + (CLInfo.getDeviceInfoInt(device, CL_DEVICE_COMPILER_AVAILABLE) != 0));

          printDeviceInfo(device, "CL_DEVICE_NAME", CL_DEVICE_NAME);
          printDeviceInfo(device, "CL_DEVICE_VENDOR", CL_DEVICE_VENDOR);
          printDeviceInfo(device, "CL_DRIVER_VERSION", CL_DRIVER_VERSION);
          printDeviceInfo(device, "CL_DEVICE_PROFILE", CL_DEVICE_PROFILE);
          printDeviceInfo(device, "CL_DEVICE_VERSION", CL_DEVICE_VERSION);
          printDeviceInfo(device, "CL_DEVICE_EXTENSIONS", CL_DEVICE_EXTENSIONS);
          if (caps.OpenCL11) {
            printDeviceInfo(device, "CL_DEVICE_OPENCL_C_VERSION", CL_DEVICE_OPENCL_C_VERSION);
          }

          CLContextCallback contextCB;
          long context =
              clCreateContext(
                  ctxProps,
                  device,
                  contextCB =
                      CLContextCallback.create(
                          (errinfo, private_info, cb, user_data) -> {
                            System.err.println("[LWJGL] cl_context_callback");
                            System.err.println("\tInfo: " + memUTF8(errinfo));
                          }),
                  NULL,
                  errcode_ret);
          CLInfo.checkCLError(errcode_ret);

          long buffer = clCreateBuffer(context, CL_MEM_READ_ONLY, 128, errcode_ret);
          CLInfo.checkCLError(errcode_ret);

          CLMemObjectDestructorCallback bufferCB1 = null;
          CLMemObjectDestructorCallback bufferCB2 = null;

          long subbuffer = NULL;

          CLMemObjectDestructorCallback subbufferCB = null;

          int errcode;

          CountDownLatch destructorLatch;

          if (caps.OpenCL11) {
            destructorLatch = new CountDownLatch(3);

            errcode =
                clSetMemObjectDestructorCallback(
                    buffer,
                    bufferCB1 =
                        CLMemObjectDestructorCallback.create(
                            (memobj, user_data) -> {
                              System.out.println("\t\tBuffer destructed (1): " + memobj);
                              destructorLatch.countDown();
                            }),
                    NULL);
            CLInfo.checkCLError(errcode);

            errcode =
                clSetMemObjectDestructorCallback(
                    buffer,
                    bufferCB2 =
                        CLMemObjectDestructorCallback.create(
                            (memobj, user_data) -> {
                              System.out.println("\t\tBuffer destructed (2): " + memobj);
                              destructorLatch.countDown();
                            }),
                    NULL);
            CLInfo.checkCLError(errcode);

            try (CLBufferRegion buffer_region = CLBufferRegion.malloc()) {
              buffer_region.origin(0);
              buffer_region.size(64);

              subbuffer =
                  nclCreateSubBuffer(
                      buffer,
                      CL_MEM_READ_ONLY,
                      CL_BUFFER_CREATE_TYPE_REGION,
                      buffer_region.address(),
                      memAddress(errcode_ret));
              CLInfo.checkCLError(errcode_ret);
            }

            errcode =
                clSetMemObjectDestructorCallback(
                    subbuffer,
                    subbufferCB =
                        CLMemObjectDestructorCallback.create(
                            (memobj, user_data) -> {
                              System.out.println("\t\tSub Buffer destructed: " + memobj);
                              destructorLatch.countDown();
                            }),
                    NULL);
            CLInfo.checkCLError(errcode);
          } else {
            destructorLatch = null;
          }

          long exec_caps = CLInfo.getDeviceInfoLong(device, CL_DEVICE_EXECUTION_CAPABILITIES);
          if ((exec_caps & CL_EXEC_NATIVE_KERNEL) == CL_EXEC_NATIVE_KERNEL) {
            System.out.println("\t\t-TRYING TO EXEC NATIVE KERNEL-");
            long queue = clCreateCommandQueue(context, device, NULL, errcode_ret);

            PointerBuffer ev = BufferUtils.createPointerBuffer(1);

            ByteBuffer kernelArgs = BufferUtils.createByteBuffer(4);
            kernelArgs.putInt(0, 1337);

            CLNativeKernel kernel;
            errcode =
                clEnqueueNativeKernel(
                    queue,
                    kernel =
                        CLNativeKernel.create(
                            args ->
                                System.out.println(
                                    "\t\tKERNEL EXEC argument: "
                                        + memByteBuffer(args, 4).getInt(0)
                                        + ", should be 1337")),
                    kernelArgs,
                    null,
                    null,
                    null,
                    ev);
            CLInfo.checkCLError(errcode);

            long e = ev.get(0);

            CountDownLatch latch = new CountDownLatch(1);

            CLEventCallback eventCB;
            errcode =
                clSetEventCallback(
                    e,
                    CL_COMPLETE,
                    eventCB =
                        CLEventCallback.create(
                            (event, event_command_exec_status, user_data) -> {
                              System.out.println(
                                  "\t\tEvent callback status: "
                                      + getEventStatusName(event_command_exec_status));
                              latch.countDown();
                            }),
                    NULL);
            CLInfo.checkCLError(errcode);

            try {
              boolean expired = !latch.await(500, TimeUnit.MILLISECONDS);
              if (expired) {
                System.out.println("\t\tKERNEL EXEC FAILED!");
              }
            } catch (InterruptedException exc) {
              exc.printStackTrace();
            }
            eventCB.free();

            errcode = clReleaseEvent(e);
            CLInfo.checkCLError(errcode);
            kernel.free();

            kernelArgs = BufferUtils.createByteBuffer(POINTER_SIZE * 2);

            kernel = CLNativeKernel.create(args -> {});

            long time = System.nanoTime();
            int REPEAT = 1000;
            for (int i = 0; i < REPEAT; i++) {
              clEnqueueNativeKernel(queue, kernel, kernelArgs, null, null, null, null);
            }
            clFinish(queue);
            time = System.nanoTime() - time;

            System.out.printf(
                "\n\t\tEMPTY NATIVE KERNEL AVG EXEC TIME: %.4fus\n",
                (double) time / (REPEAT * 1000));

            errcode = clReleaseCommandQueue(queue);
            CLInfo.checkCLError(errcode);
            kernel.free();
          }

          System.out.println();

          if (subbuffer != NULL) {
            errcode = clReleaseMemObject(subbuffer);
            CLInfo.checkCLError(errcode);
          }

          errcode = clReleaseMemObject(buffer);
          CLInfo.checkCLError(errcode);

          if (destructorLatch != null) {
            // mem object destructor callbacks are called asynchronously on Nvidia

            try {
              destructorLatch.await();
            } catch (InterruptedException e) {
              e.printStackTrace();
            }

            subbufferCB.free();

            bufferCB2.free();
            bufferCB1.free();
          }

          errcode = clReleaseContext(context);
          CLInfo.checkCLError(errcode);

          contextCB.free();
        }
      }
    } catch (OpenCLException ex) {
      logger.error(ex.getMessage());
      throw ex;
    }
  }

  public static void get(FunctionProviderLocal provider, long platform, String name) {
    System.out.println(name + ": " + provider.getFunctionAddress(platform, name));
  }

  private static void printPlatformInfo(long platform, String param_name, int param)
      throws OpenCLException {
    System.out.println(
        "\t" + param_name + " = " + CLInfo.getPlatformInfoStringUTF8(platform, param));
  }

  private static void printDeviceInfo(long device, String param_name, int param)
      throws OpenCLException {
    System.out.println("\t" + param_name + " = " + CLInfo.getDeviceInfoStringUTF8(device, param));
  }

  private static String getEventStatusName(int status) {
    switch (status) {
      case CL_QUEUED:
        return "CL_QUEUED";
      case CL_SUBMITTED:
        return "CL_SUBMITTED";
      case CL_RUNNING:
        return "CL_RUNNING";
      case CL_COMPLETE:
        return "CL_COMPLETE";
      default:
        throw new IllegalArgumentException(String.format("Unsupported event status: 0x%X", status));
    }
  }
}
