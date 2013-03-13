# JAR Enumerator
Command line utility that extracts the methods of classes in a JAR file. Filtering by a variety of class types and modifiers is supported. Useful if you ever need to know what is in a JAR - especially when you want to explore a public API and don't have the Javadocs.	

# Building

	ant clean build dist

# Using

	java -jar jar-enumerator.jar android.jar -skipNative -skipVarargs -skipInterface -skipAbstract -skipNonStatic -skipMethodsWithNonConcreteParams

Usage:

	java -jar dist/jar-enumerator.jar -h
	usage: jar-enumerator <input JAR> [zero or more filtering args listed below]
         [-skipStatic]
            Skips static methods
         [-skipNonStatic]
            Skips non-static methods
         [-skipFinal]
            Skips final methods
         [-skipNative]
            Skips native methods
         [-skipSynthetic]
            Skips synthetic methods
         [-skipVarargs]
            Skips methods that have a varargs parameter
         [-skipBridge]
            Skips bridge methods
         [-skipInterface]
            Skips interface classes
         [-skipAbstract]
            Skips abstract classes
         [-skipMethodsWithNonConcreteParams]
            Skips any methods that have non-concrete (abstract, interface, etc.) parameters
         [-showModifiers]
            Shows return types and modifiers (public, abstract, etc.) of methods when printing.
				  
