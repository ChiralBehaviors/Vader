/** 
 * (C) Copyright 2011 Hal Hildebrand, all rights reserved.
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package com.hellblazer.process;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;

/**
 * 
 * @author Hal Hildebrand
 * 
 */
public class Utils {

    public static Object accessField(String fieldName, Object target)
                                                                     throws SecurityException,
                                                                     NoSuchFieldException,
                                                                     IllegalArgumentException,
                                                                     IllegalAccessException {
        Field field;
        try {
            field = target.getClass().getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            Class<?> superClass = target.getClass().getSuperclass();
            if (superClass == null) {
                throw e;
            }
            return accessField(fieldName, target, superClass);
        }
        field.setAccessible(true);
        return field.get(target);
    }

    public static Object accessField(String fieldName, Object target,
                                     Class<?> targetClass)
                                                          throws SecurityException,
                                                          NoSuchFieldException,
                                                          IllegalArgumentException,
                                                          IllegalAccessException {
        Field field;
        try {
            field = targetClass.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            Class<?> superClass = targetClass.getSuperclass();
            if (superClass == null) {
                throw e;
            }
            return accessField(fieldName, target, superClass);
        }
        field.setAccessible(true);
        return field.get(target);
    }

    public static void copy(InputStream in, OutputStream out)
                                                             throws IOException {
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
    }

    public static void copyDirectory(File sourceLocation, File targetLocation)
                                                                              throws IOException {

        if (sourceLocation.isDirectory()) {
            if (!targetLocation.exists()) {
                targetLocation.mkdir();
            }

            String[] children = sourceLocation.list();
            for (String element : children) {
                copyDirectory(new File(sourceLocation, element),
                              new File(targetLocation, element));
            }
        } else {

            InputStream in = new FileInputStream(sourceLocation);
            OutputStream out = new FileOutputStream(targetLocation);

            // Copy the bits from instream to outstream
            copy(in, out);
            in.close();
            out.close();
        }
    }

    public static void initializeDirectory(File directory) {
        remove(directory);
        if (!directory.mkdirs()) {
            throw new IllegalStateException("Cannot create directtory: "
                                            + directory);
        }
    }

    public static void initializeDirectory(String dir) {
        initializeDirectory(new File(dir));
    }

    public static void remove(File directory) {
        if (directory.exists()) {
            for (File file : directory.listFiles()) {
                if (file.isDirectory()) {
                    remove(file);
                } else {
                    if (!file.delete()) {
                        throw new IllegalStateException("Cannot delete file: "
                                                        + file);
                    }
                }
            }
            if (!directory.delete()) {
                throw new IllegalStateException("Cannot delete directory: "
                                                + directory);
            }
        }
    }

    public static void setField(String fieldName, Object target, Object value)
                                                                              throws IllegalArgumentException,
                                                                              IllegalAccessException,
                                                                              NoSuchFieldException {
        Field field;
        try {
            field = target.getClass().getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            Class<?> superClass = target.getClass().getSuperclass();
            if (superClass == null) {
                throw e;
            }
            setField(fieldName, target, value, superClass);
            return;
        }
        field.setAccessible(true);
        field.set(target, value);
    }

    public static void setField(String fieldName, Object target, Object value,
                                Class<?> targetClass)
                                                     throws NoSuchFieldException,
                                                     IllegalArgumentException,
                                                     IllegalAccessException {
        Field field;
        try {
            field = targetClass.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            Class<?> superClass = targetClass.getSuperclass();
            if (superClass == null) {
                throw e;
            }
            setField(fieldName, target, superClass);
            return;
        }
        field.setAccessible(true);
        field.set(target, value);
    }

    public static boolean waitForCondition(int maxWaitTime, Condition condition) {
        return waitForCondition(maxWaitTime, 100, condition);
    }

    public static boolean waitForCondition(int maxWaitTime,
                                           final int sleepTime,
                                           Condition condition) {
        long endTime = System.currentTimeMillis() + maxWaitTime;
        while (System.currentTimeMillis() < endTime) {
            if (condition.isTrue()) {
                return true;
            }
            try {
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                // do nothing
            }
        }
        return false;
    }
}