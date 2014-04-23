package mergetool;

import japa.parser.ast.CompilationUnit;
import japa.parser.ast.ImportDeclaration;
import japa.parser.ast.body.ConstructorDeclaration;
import japa.parser.ast.body.FieldDeclaration;
import japa.parser.ast.body.MethodDeclaration;
import japa.parser.ast.body.Parameter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import mergetool.MergeToolInput.InputException;

public class MergeTool {
    private ClassDeclarations classADeclarations;
    private ClassDeclarations classBDeclarations;
    
    private String classAPackage;
    private String classBPackage;
    
    private String classAType;
    private String classBType;
    
    private String classAName;
    private String classBName;
    
    private String className;
    private String aspectName;
    
    // Configuration file input
    private final CompilationUnit classA;
    private final CompilationUnit classB;
    private final boolean mergeFieldsByName;
    private final List<String> fieldNamesToMerge;
    private final List<String> methodNamesToMerge;
    private final List<Boolean> methodNamesToMergeOrder;
    private final List<String> methodNamesToOverride;
    private final List<Boolean> overrideClassAWithClassBChoices;
    
    public MergeTool(MergeToolInput input) {
        // Transfer input from configuration file
        classA = input.classA;
        classB = input.classB;
        mergeFieldsByName = input.mergeAllFieldsByName;
        fieldNamesToMerge = input.fieldNamesToMerge;
        methodNamesToMerge = input.methodNamesToMerge;
        methodNamesToMergeOrder = input.methodNamesToMergeOrder;
        methodNamesToOverride = input.methodNamesToOverride;
        overrideClassAWithClassBChoices = input.overrideClassAWithClassBChoices;
        
        classADeclarations = new ClassDeclarations(classA);
        classBDeclarations = new ClassDeclarations(classB);
        
        classAPackage = classA.getPackage().getName().toString();
        classBPackage = classB.getPackage().getName().toString();
        
        String aName = classA.getTypes().get(0).getName();
        String bName = classB.getTypes().get(0).getName();
        
        classAType = classAPackage + "." + aName;
        classBType = classBPackage + "." + bName;
        
        classAName = classAPackage + aName;
        classBName = classBPackage + bName;
        
        className = aName;
        aspectName = "Merge" + className;
    }
    
    public void writeAspectToFile() {
        String aspect = generateAspect();
        BufferedWriter out = null;
        try {
            out = new BufferedWriter(new FileWriter(new File("src/" + aspectName + ".aj")));
            out.write(aspect);
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public String generateAspect() {
        String aspect = new String();
        
        aspect += generateMergedImports();
        aspect += "public privileged aspect " + aspectName + " {\n";
        aspect += "\n";
        aspect += generateInstanceFields();
        aspect += "\n";
        aspect += generateMergedConstructor();
        aspect += "\n";
        aspect += generateMergedFields();
        aspect += generateOverriddenMethods();
        aspect += generateMergedMethods();
        aspect += "}\n";
        
        return aspect;
    }
    
    private String generateMergedImports() {
        Set<ImportDeclaration> importDeclarationSet = new HashSet<>();
        
        for (ImportDeclaration importDeclaration : classA.getImports()) {
            importDeclarationSet.add(importDeclaration);
        }
        
        for (ImportDeclaration importDeclaration : classB.getImports()) {
            importDeclarationSet.add(importDeclaration);
        }
        
        String imports = new String();
        
        for (ImportDeclaration importDeclaration : importDeclarationSet) {
            imports += importDeclaration + "\n";
        }
        
        return imports;
    }
    
    private String generateInstanceFields() {
        String instanceFields = new String();
        
        instanceFields += "private " + classAType + " " + classAName + ";\n";
        instanceFields += "private " + classBType + " " + classBName + ";\n";
        
        return instanceFields;
    }
    
    private String generateMergedConstructor() {
        String constructors = new String();
        
        constructors += generateMergedConstructorPointcuts();
        constructors += "\n";
        constructors += generateMergedConstructorJoinPoints();
        constructors += "\n";
        constructors += generateMergedConstructorAdvices();
        
        return constructors;
    }
    
    private String generateMergedConstructorPointcuts() {
        String constructorPointcuts = new String();
        
        List<Parameter> classAConstructorParameters = classADeclarations.getConstructorDeclarations().get(0).getParameters();
        List<Parameter> classBConstructorParameters = classBDeclarations.getConstructorDeclarations().get(0).getParameters();
        
        constructorPointcuts += Generator.generateMergedConstructorPointcut(classAName, classAType, classAConstructorParameters, aspectName);
        constructorPointcuts += "\n";
        constructorPointcuts += Generator.generateMergedConstructorPointcut(classBName, classBType, classBConstructorParameters, aspectName);
        
        return constructorPointcuts;
    }
    
    private String generateMergedConstructorJoinPoints() {
        String constructorJoinPoints = new String();
        
        List<Parameter> classAConstructorParameters = classADeclarations.getConstructorDeclarations().get(0).getParameters();
        List<Parameter> classBConstructorParameters = classBDeclarations.getConstructorDeclarations().get(0).getParameters();
        
        constructorJoinPoints += Generator.generateMergedConstructorJoinPoint(classAType, classAName, classAConstructorParameters);
        constructorJoinPoints += "\n";
        constructorJoinPoints += Generator.generateMergedConstructorJoinPoint(classBType, classBName, classBConstructorParameters);
        
        return constructorJoinPoints;
    }
    
    private String generateMergedConstructorAdvices() {
        String constructorsAdvice = new String();
        
        for (ConstructorDeclaration constructor : classADeclarations.getConstructorDeclarations()) {
            constructorsAdvice += generateMergedConstructorAdvice(constructor);
        }
        
        return constructorsAdvice;
    }
    
    private String generateMergedConstructorAdvice(ConstructorDeclaration constructor) {
        String constructorAdvice = new String();
        
        List<Parameter> parameters = constructor.getParameters();
        constructorAdvice += Generator.generateMergedConstructorAdvice(className, classAName, classAPackage, classBName, classBPackage, parameters);
        constructorAdvice += "\n";
        constructorAdvice += Generator.generateMergedConstructorAdvice(className, classBName, classBPackage, classAName, classAPackage, parameters);
        
        return constructorAdvice;
    }
    
    private String generateMergedFields() {
        String mergedFields = new String();
        
        List<FieldDeclaration> fieldDeclarations;
        
        if (mergeFieldsByName) {
            fieldDeclarations = DeclarationConverter.unionFieldDeclarations(classADeclarations.getFieldDeclarations(), classBDeclarations.getFieldDeclarations());
        } else {
            fieldDeclarations = new ArrayList<>();
            for (String fieldNameToMerge : fieldNamesToMerge) {
                fieldDeclarations.add(classADeclarations.getFieldDeclarationForName(fieldNameToMerge));
            }
        }
        
        for (FieldDeclaration fieldDeclaration : fieldDeclarations) {
            String fieldName = fieldDeclaration.getVariables().get(0).toString();
            String replaceWithType = fieldDeclaration.getType().toString();
            String replaceFieldName = classBPackage + "." + className + "." + fieldName;
            String replaceWithFieldName = classAPackage + "." + className + "." + fieldName;
            String aspectFieldName = "this." + classAName + "." + fieldName;
            mergedFields += Generator.generateMergedField(aspectName, replaceWithType, replaceFieldName, replaceWithFieldName, aspectFieldName) + "\n";
        }
        
        return mergedFields;
    }
    
    private String generateOverriddenMethods() {
        String overriddenMethods = new String();
        
        for (int i = 0; i < methodNamesToOverride.size(); i++) {
            String methodName = methodNamesToOverride.get(i);
            boolean aOrB = overrideClassAWithClassBChoices.get(i).booleanValue();
            MethodDeclaration md = classADeclarations.getMethodDeclarationForName(methodName);
            String returnType = md.getType().toString();
            String overrideMethodName = (aOrB ? classBType : classAType) + "." + methodName;
            String overrideWithMethodName = (aOrB ? classAType : classBType) + "." + methodName;
            List<Parameter> parameters = md.getParameters();
            String aspectMethodName = "this." + (aOrB ? classAName : classBName) + "." + methodName;
            overriddenMethods += Generator.generateOverriddenMethod(returnType, overrideMethodName, overrideWithMethodName, parameters, aspectMethodName) + "\n";
        }
        
        return overriddenMethods;
    }
    
    private String generateMergedMethods() {
        String mergedMethods = new String();
        
        List<MethodDeclaration> methodsToMerge = new ArrayList<>();
        for (String methodNameToMerge : methodNamesToMerge) {
            methodsToMerge.add(classADeclarations.getMethodDeclarationForName(methodNameToMerge));
        }
        
        for (int i = 0; i < methodsToMerge.size(); i++) {
            MethodDeclaration methodDeclaration = methodsToMerge.get(i);
            boolean caobf = methodNamesToMergeOrder.get(i);
            String classType = (caobf ? classAType : classBType);
            String className = (caobf ? classBName : classAName);
            mergedMethods += Generator.generateMergedMethod(methodDeclaration, classType, className, aspectName);
            mergedMethods += "\n";
        }
        
        return mergedMethods;
    }
    
    public static void main(String[] args) {
        MergeToolInput input;
        try {
            input = new MergeToolInput("src/input.json");
        } catch (InputException e) {
            e.printStackTrace();
            return;
        }
        
        MergeTool tool = new MergeTool(input);
        tool.writeAspectToFile();
        
        System.out.println("done");
    }
    
}
