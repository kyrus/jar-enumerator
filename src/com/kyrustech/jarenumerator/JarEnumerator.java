package com.kyrustech.jarenumerator;

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

/**
 * Dumps the methods of classes in a JAR file
 */
public class JarEnumerator {
    /**
     * Loads classes from a JAR file
     * @param resource JAR file to load
     * @return List of classes that have been loaded
     */
    private static List<Class<?>> loadClassesFromJar(URL resource) {
        // create a custom classloader with the JAR we are loading 
        URLClassLoader customLoader = new URLClassLoader(new URL[] { resource }, JarEnumerator.class.getClassLoader());

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
                try {
                    classes.add(Class.forName(className, true, customLoader));
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException("Unexpected ClassNotFoundException loading class '" + className + "'");
                }
            }
        }
        return classes;
    }

    /**
     * Loads methods in a class
     * @param clazz class to process
     * @param skipStatic
     * @param skipNonStatic
     * @param skipFinal
     * @param skipNative
     * @param skipSynthetic
     * @param skipVarargs
     * @param skipBridge
     * @param skipInterface
     * @param skipAbstract
     * @param skipMethodsWithNonConcreteParams
     * @param showModifiers
     * @return List of strings describing methods in the class that pass the filter 
     */
    private static List<String> loadClassMethods(Class<?> clazz, boolean skipStatic, boolean skipNonStatic,
            boolean skipFinal, boolean skipNative, boolean skipSynthetic, boolean skipVarargs, boolean skipBridge,
            boolean skipInterface, boolean skipAbstract, boolean skipMethodsWithNonConcreteParams, boolean showModifiers) {
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

            // convert the class+method to a custom string format and add it to our return list
            ret.add(toDescriptorString(clazz, m, showModifiers));
        }
        return ret;
    }
    
    /**
     * Creates a string describing a method in a class
     * @param clazz Class that contains the method
     * @param m method to describe
     * @param showModifiers if the method's modifiers should be printed
     * @return string describing the method
     */
    private static String toDescriptorString(Class<?> clazz, Method m, boolean showModifiers) {
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
            System.err.println("usage: jar-dumper <input JAR> [zero or more filtering args listed below]");
            System.err.println("         [-skipStatic]");
            System.err.println("            Skips static methods");
            System.err.println("         [-skipNonStatic]");
            System.err.println("            Skips non-static methods");
            System.err.println("         [-skipFinal]");
            System.err.println("            Skips final methods");
            System.err.println("         [-skipNative]");
            System.err.println("            Skips native methods");
            System.err.println("         [-skipSynthetic]");
            System.err.println("            Skips synthetic methods");
            System.err.println("         [-skipVarargs]");
            System.err.println("            Skips methods that have a varargs parameter");
            System.err.println("         [-skipBridge]");
            System.err.println("            Skips bridge methods");
            System.err.println("         [-skipInterface]");
            System.err.println("            Skips interface classes");
            System.err.println("         [-skipAbstract]");
            System.err.println("            Skips abstract classes");
            System.err.println("         [-skipMethodsWithNonConcreteParams]");
            System.err
                    .println("            Skips any methods that have non-concrete (abstract, interface, etc.) parameters");
            System.err.println("         [-showModifiers]");
            System.err
                    .println("            Shows return types and modifiers (public, abstract, etc.) of methods when printing.");
            System.err
                    .println("\nExample: java -jar jar-dumper.jar android.jar -skipNative -skipVarargs -skipInterface -skipAbstract -skipNonStatic -skipMethodsWithNonConcreteParams");
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
        boolean skipInterface = argList.contains("-skipInterface");
        boolean skipAbstract = argList.contains("-skipAbstract");
        boolean showModifiers = argList.contains("-showModifiers");
        boolean skipMethodsWithNonConcreteParams = argList.contains("-skipMethodsWithNonConcreteParams");

        try {
            // load classes from the jar and sort them by name
            List<Class<?>> classes = loadClassesFromJar(new File(inputJar).toURI().toURL());
            Collections.sort(classes, new Comparator<Class<?>>() {
                @Override
                public int compare(Class<?> o1, Class<?> o2) {
                    return o1.getCanonicalName().compareTo(o2.getCanonicalName());
                }
            });

            // process each loaded class
            for (Class<?> c : classes) {
                for (String s : loadClassMethods(c, skipStatic, skipNonStatic, skipFinal, skipNative, skipSynthetic,
                        skipVarargs, skipBridge, skipInterface, skipAbstract, skipMethodsWithNonConcreteParams,
                        showModifiers)) {
                    // output the method descriptor
                    System.out.println(s);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(1);
        }
    }
}
