package com.kyrus.jardumper;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class JarDumper {
    private static URLClassLoader customLoader;

    private static Class<?> loadClass(String className) {
        try {
            return Class.forName(className, true, customLoader);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Unexpected ClassNotFoundException loading class '" + className + "'");
        }
    }

    private static List<Class<?>> processJarfile(URL resource) {
        // add the jar to the classpath
        customLoader = new URLClassLoader(new URL[] { resource }, customLoader != null ? customLoader
                : JarDumper.class.getClassLoader());

        String jarPath = resource.getPath().replaceFirst("[.]jar[!].*", ".jar").replaceFirst("file:", "");

        JarFile jarFile;
        try {
            jarFile = new JarFile(jarPath);
        } catch (IOException e) {
            throw new RuntimeException("Unexpected IOException reading JAR File '" + jarPath + "'", e);
        }

        ArrayList<Class<?>> classes = new ArrayList<Class<?>>();
        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            String entryName = entries.nextElement().getName();
            if (entryName.endsWith(".class")) {
                String className = entryName.replace('/', '.').replace('\\', '.').replace(".class", "");
                // System.out.println("Found class in jar [" + className + "]");
                classes.add(loadClass(className));
            }
        }
        return classes;
    }

    private static List<String> loadClassMethods(Class<?> clazz, boolean skipStatic, boolean skipNonStatic,
            boolean skipFinal, boolean skipNative, boolean skipSynthetic, boolean skipVarargs, boolean skipBridge,
            boolean skipVolatile, boolean skipTransient, boolean skipInterface, boolean skipAbstract,
            boolean skipMethodsWithNonConcreteParams, boolean showModifiers) {
        List<String> ret = new ArrayList<String>();
        for (Method m : clazz.getMethods()) {
            if (skipStatic && Modifier.isStatic(m.getModifiers()))
                continue;
            if (skipNonStatic && !Modifier.isStatic(m.getModifiers()))
                continue;
            if (skipFinal && Modifier.isFinal(m.getModifiers()))
                continue;
            if (skipNative && Modifier.isNative(m.getModifiers()))
                continue;
            if (skipSynthetic && m.isSynthetic())
                continue;
            if (skipVarargs && m.isVarArgs())
                continue;
            if (skipBridge && m.isBridge())
                continue;
            if (skipVolatile && Modifier.isVolatile(m.getModifiers()))
                continue;
            if (skipTransient && Modifier.isTransient(m.getModifiers()))
                continue;
            if (skipInterface && Modifier.isInterface(m.getModifiers()))
                continue;
            if (skipAbstract && Modifier.isAbstract(m.getModifiers()))
                continue;

            if (skipMethodsWithNonConcreteParams) {
                boolean hasNonConcreteParam = false;
                for (Class<?> paramType : m.getParameterTypes()) {
                    if (paramType.isInterface() || Modifier.isAbstract(paramType.getModifiers())
                            || Modifier.isInterface(paramType.getModifiers())) {
                        hasNonConcreteParam = true;
                        break;
                    }
                }
                if (hasNonConcreteParam)
                    continue;
            }

            ret.add(toSootForm(clazz, m, showModifiers));
        }
        return ret;
    }

    private static String toSootForm(Class<?> clazz, Method m, boolean showModifiers) {
        StringBuilder ret = new StringBuilder();
        // class name
        ret.append("<").append(clazz.getCanonicalName()).append(": ");
        if (showModifiers) {
            // modifier
            ret.append(Modifier.toString(m.getModifiers())).append(" ");
        }
        // return type
        ret.append(m.getReturnType().getCanonicalName()).append(" ");
        // name
        ret.append(m.getName());
        // args
        ret.append("(");
        for (Class<?> arg : m.getParameterTypes()) {
            ret.append(arg.getCanonicalName()).append(",");
        }
        if (m.getParameterTypes().length != 0) {
            // remove the final comma
            ret.deleteCharAt(ret.length() - 1);
        }
        ret.append(")>");
        return ret.toString();
    }

    public static void main(String[] args) {
        if (args.length < 1 || Arrays.asList(args).contains("-h")) {
            System.err.println("usage: jar-dumper <input JAR>");
            System.err.println("                  [-skipStatic]");
            System.err.println("                  [-skipNonStatic]");
            System.err.println("                  [-skipFinal]");
            System.err.println("                  [-skipNative]");
            System.err.println("                  [-skipSynthetic]");
            System.err.println("                  [-skipVarargs]");
            System.err.println("                  [-skipBridge]");
            System.err.println("                  [-skipVolatile]");
            System.err.println("                  [-skipTransient]");
            System.err.println("                  [-skipInterface]");
            System.err.println("                  [-skipAbstract]");
            System.err.println("                  [-skipMethodsWithNonConcreteParams]");
            System.err.println("                  [-showModifiers]");
            System.err
                    .println("\nExample: JarDumper C:\\android-sdks\\platforms\\android-10\\android.jar -skipNative -skipVarargs -skipInterface -skipAbstract -skipNonStatic -skipMethodsWithNonConcreteParams");
            System.exit(2);
        }
        String inputJar = args[0];
        List<String> argList = Arrays.asList(args);

        boolean skipStatic = argList.contains("-skipStatic");
        boolean skipNonStatic = argList.contains("-skipNonStatic");
        boolean skipFinal = argList.contains("-skipFinal");
        boolean skipNative = argList.contains("-skipNative");
        boolean skipSynthetic = argList.contains("-skipSynthetic");
        boolean skipVarargs = argList.contains("-skipVarargs");
        boolean skipBridge = argList.contains("-skipBridge");
        boolean skipVolatile = argList.contains("-skipVolatile");
        boolean skipTransient = argList.contains("-skipTransient");
        boolean skipInterface = argList.contains("-skipInterface");
        boolean skipAbstract = argList.contains("-skipAbstract");
        boolean showModifiers = argList.contains("-showModifiers");
        boolean skipMethodsWithNonConcreteParams = argList.contains("-skipMethodsWithNonConcreteParams");

        try {
            List<Class<?>> classes = processJarfile(new File(inputJar).toURI().toURL());

            Collections.sort(classes, new Comparator<Class<?>>() {
                @Override
                public int compare(Class<?> o1, Class<?> o2) {
                    return o1.getCanonicalName().compareTo(o2.getCanonicalName());
                }
            });

            for (Class<?> c : classes) {
                for (String s : loadClassMethods(c, skipStatic, skipNonStatic, skipFinal, skipNative, skipSynthetic,
                        skipVarargs, skipBridge, skipVolatile, skipTransient, skipInterface, skipAbstract,
                        skipMethodsWithNonConcreteParams, showModifiers)) {
                    System.out.println(s);
                }
            }
            // System.out.println("Reported " + classes.size() + " classes and "
            // + numMethods + " methods");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
