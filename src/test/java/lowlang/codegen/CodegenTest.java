package lowlang.codegen;

import lowlang.tokenizer.Tokenizer;
import lowlang.tokenizer.TokenizerException;
import lowlang.parser.Parser;
import lowlang.parser.Join;
import lowlang.parser.ParseException;
import lowlang.parser.Program;
import lowlang.typechecker.Typechecker;
import lowlang.typechecker.TypeErrorException;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertArrayEquals;
import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.TestName;

public class CodegenTest {
    @Rule public TestName name = new TestName();

    public static int[] parseOutput(final String[] spimOutput) {
        final int[] retval = new int[spimOutput.length];
        for (int index = 0; index < retval.length; index++) {
            retval[index] = Integer.parseInt(spimOutput[index]);
        }
        return retval;
    } // parseOutput

    public void assertResult(final String programAsString,
                             final int... expected) throws TokenizerException, ParseException, TypeErrorException, IOException {
        boolean wantToSaveFile = true; // for debugging

        final Program program = Parser.parse(Tokenizer.tokenize(programAsString));
        Typechecker.typecheckProgramExternalEntry(program);
        final File file = File.createTempFile(name.getMethodName(),
                                              ".asm",
                                              new File("testPrograms"));
        boolean testPassed = false;
        try {
            MIPSCodeGenerator.compile(program, file);
            final String[] output = SPIMRunner.runFile(file);
            final int[] received = parseOutput(output);
            if (wantToSaveFile) {
                assertArrayEquals("Expected: [" +
                                  Join.join(", ", expected) +
                                  "]; Received: [" +
                                  Join.join(", ", received) +
                                  "; File: " +
                                  file.getAbsolutePath(),
                                  expected,
                                  received);
            } else {
                assertArrayEquals(expected, received);
            }
            testPassed = true;
        } finally {
            if (!wantToSaveFile || testPassed) {
                file.delete();
            }
        }
    }

    // ---BEGIN TESTS FOR EXPRESSIONS---
    public void assertExpResult(final String expString,
                                final int expected) throws TokenizerException, ParseException, TypeErrorException, IOException{
        assertResult("void main() { print(" + expString + "); }",
                     expected);
    }
    
    @Test
    public void testIntLiteral() throws Exception {
        assertExpResult("1", 1);
    }

    @Test
    public void testBoolLiteralTrue() throws Exception {
        assertExpResult("true", 1);
    }

    @Test
    public void testBoolLiteralFalse() throws Exception {
        assertExpResult("false", 0);
    }

    @Test
    public void testEqualsIntTrue() throws Exception {
        assertExpResult("42 == 42", 1);
    }

    @Test
    public void testEqualsIntFalse() throws Exception {
        assertExpResult("42 == 43", 0);
    }

    @Test
    public void testEqualsBoolTrue() throws Exception {
        assertExpResult("false == false", 1);
    }

    @Test
    public void testEqualsBoolFalse() throws Exception {
        assertExpResult("true == false", 0);
    }

    @Test
    public void testPlus() throws Exception {
        assertExpResult("2 + 3", 5);
    }

    @Test
    public void testMinus() throws Exception {
        assertExpResult("3 - 2", 1);
    }

    @Test
    public void testMult() throws Exception {
        assertExpResult("3 * 4", 12);
    }

    @Test
    public void testDiv() throws Exception {
        assertExpResult("6 / 2", 3);
    }

    @Test
    public void testSizeofInt() throws Exception {
        assertExpResult("sizeof(int)", 4);
    }

    @Test
    public void testSizeofBool() throws Exception {
        assertExpResult("sizeof(bool)", 4);
    }

    @Test
    public void testSizeofPointer() throws Exception {
        assertExpResult("sizeof(int*)", 4);
    }

    @Test
    public void testDereference() throws Exception {
        // TODO: this is very bad; assumes initial value of allocated memory
        assertExpResult("*((int*)malloc(4))", 0);
    }
    // ---END TESTS FOR EXPRESSIONS---

    // ---BEGIN TESTS FOR STATEMENTS---
    @Test
    public void testStructureAccessSingleField() throws Exception {
        assertResult("struct Foo { int x; };" +
                     "void main() { print(Foo(7).x); }",
                     7);
    }

    @Test
    public void testStructureAccessTwoFieldFirst() throws Exception {
        assertResult("struct Foo { int x; int y; };" +
                     "void main() { print(Foo(1, 2).x); }",
                     1);
    }

    @Test
    public void testStructureAccessTwoFieldSecond() throws Exception {
        assertResult("struct Foo { int x; int y; };" +
                     "void main() { print(Foo(1, 2).y); }",
                     2);
    }

    @Test
    public void testStructureAccessNestedStructureFirst() throws Exception {
        assertResult("struct Bar { int x; int y; };" +
                     "struct Foo { Bar f; int z; };" +
                     "void main() { print(Foo(Bar(1, 2), 3).f.y); }",
                     2);
    }

    @Test
    public void testStructureAccessNestedStructureSecond() throws Exception {
        assertResult("struct Bar { int x; int y; };" +
                     "struct Foo { int z; Bar f; };" +
                     "void main() { print(Foo(1, Bar(2, 3)).f.x); }",
                     2);
    }

    @Test
    public void testSingleIntVariableDeclaration() throws Exception {
        assertResult("void main() {" +
                     "  int x = 1;" +
                     "  print(x);" +
                     "}",
                     1);
    }

    @Test
    public void testDoubleIntVariableDeclaration() throws Exception {
        assertResult("void main() {" +
                     "  int x = 1;" +
                     "  int y = 2;" +
                     "  print(x);" +
                     "  print(y);" +
                     "}",
                     1,
                     2);
    }

    @Test
    public void testDoubleIntVariableDeclarationGetSecond() throws Exception {
        assertResult("void main() {" +
                     "  int x = 1;" +
                     "  int y = 2;" +
                     "  print(y);" +
                     "}",
                     2);
    }

    @Test
    public void testSingleIntAssignment() throws Exception {
        assertResult("void main() {" +
                     "  int x = 1;" +
                     "  x = 2;" +
                     "  print(x);" +
                     "}",
                     2);
    }

    @Test
    public void testTwoIntsAssignFirst() throws Exception {
        assertResult("void main() {" +
                     "  int x = 1;" +
                     "  int y = 2;" +
                     "  x = 3;" +
                     "  print(x);" +
                     "}",
                     3);
    }

    @Test
    public void testTwoIntsAssignSecond() throws Exception {
        assertResult("void main() {" +
                     "  int x = 1;" +
                     "  int y = 2;" +
                     "  y = 3;" +
                     "  print(y);" +
                     "}",
                     3);
    }

    @Test
    public void testDeclareStructureGetFirst() throws Exception {
        assertResult("struct TwoInts { int x; int y; };" +
                     "void main() {" +
                     "  TwoInts x = TwoInts(1, 2);" +
                     "  print(x.x);" +
                     "}",
                     1);
    }

    @Test
    public void testDeclareStructureGetSecond() throws Exception {
        assertResult("struct TwoInts { int x; int y; };" +
                     "void main() {" +
                     "  TwoInts x = TwoInts(1, 2);" +
                     "  print(x.y);" +
                     "}",
                     2);
    }

    @Test
    public void testAssignSingleStructureGetFirst() throws Exception {
        assertResult("struct TwoInts { int x; int y; };" +
                     "void main() {" +
                     "  TwoInts x = TwoInts(1, 2);" +
                     "  x = TwoInts(3, 4);" +
                     "  print(x.x);" +
                     "}",
                     3);
    }

    @Test
    public void testAssignSingleStructureGetSecond() throws Exception {
        assertResult("struct TwoInts { int x; int y; };" +
                     "void main() {" +
                     "  TwoInts x = TwoInts(1, 2);" +
                     "  x = TwoInts(3, 4);" +
                     "  print(x.y);" +
                     "}",
                     4);
    }

    @Test
    public void testAssignStructureFieldFirst() throws Exception {
        assertResult("struct TwoInts { int x; int y; };" +
                     "void main() {" +
                     "  TwoInts x = TwoInts(1, 2);" +
                     "  x.x = 3;" +
                     "  print(x.x);" +
                     "}",
                     3);
    }

    @Test
    public void testAssignStructureFieldSecond() throws Exception {
        assertResult("struct TwoInts { int x; int y; };" +
                     "void main() {" +
                     "  TwoInts x = TwoInts(1, 2);" +
                     "  x.y = 3;" +
                     "  print(x.y);" +
                     "}",
                     3);
    }

    @Test
    public void testAssignNestedStructureFirst() throws Exception {
        assertResult("struct TwoInts { int x; int y; };" +
                     "struct FourInts { TwoInts first; TwoInts second; };" +
                     "void main() {" +
                     "  TwoInts first = TwoInts(1, 2);" +
                     "  TwoInts second = TwoInts(3, 4);" +
                     "  FourInts x = FourInts(first, second);" +
                     "  x.first = TwoInts(5, 6);" +
                     "  print(x.first.x);" +
                     "}",
                     5);
    }

    @Test
    public void testAssignNestedStructureSecond() throws Exception {
        assertResult("struct TwoInts { int x; int y; };" +
                     "struct FourInts { TwoInts first; TwoInts second; };" +
                     "void main() {" +
                     "  TwoInts first = TwoInts(1, 2);" +
                     "  TwoInts second = TwoInts(3, 4);" +
                     "  FourInts x = FourInts(first, second);" +
                     "  x.second = TwoInts(5, 6);" +
                     "  print(x.second.x);" +
                     "}",
                     5);
    }

    @Test
    public void testAssignIntThroughPointer() throws Exception {
        assertResult("void main() {" +
                     "  int x = 5;" +
                     "  int* p = &x;" +
                     "  *p = 7;" +
                     "  print(x);" +
                     "}",
                     7);
    }

    @Test
    public void testAssignFirstFieldThroughPointer() throws Exception {
        assertResult("struct TwoInts { int x; int y; };" +
                     "void main() {" +
                     "  TwoInts x = TwoInts(1, 2);" +
                     "  int* p = &x.x;" +
                     "  *p = 3;" +
                     "  print(x.x);" +
                     "}",
                     3);
    }

    @Test
    public void testAssignSecondFieldThroughPointer() throws Exception {
        assertResult("struct TwoInts { int x; int y; };" +
                     "void main() {" +
                     "  TwoInts x = TwoInts(1, 2);" +
                     "  int* p = &x.y;" +
                     "  *p = 3;" +
                     "  print(x.y);" +
                     "}",
                     3);
    }

    @Test
    public void testAssignStructureThroughPointer() throws Exception {
        assertResult("struct TwoInts { int x; int y; };" +
                     "void main() {" +
                     "  TwoInts x = TwoInts(1, 2);" +
                     "  TwoInts* p = &x;" +
                     "  *p = TwoInts(3, 4);" +
                     "  print(x.x);" +
                     "}",
                     3);
    }

    @Test
    public void testAssignNestedStructureThroughPointerFirst() throws Exception {
        assertResult("struct TwoInts { int x; int y; };" +
                     "struct FourInts { TwoInts first; TwoInts second; };" +
                     "void main() {" +
                     "  FourInts x = FourInts(TwoInts(1, 2), TwoInts(3, 4));" +
                     "  TwoInts* p = &x.first;" +
                     "  *p = TwoInts(5, 6);" +
                     "  print(x.first.y);" +
                     "}",
                     6);
    }

    @Test
    public void testAssignNestedStructureThroughPointerSecond() throws Exception {
        assertResult("struct TwoInts { int x; int y; };" +
                     "struct FourInts { TwoInts first; TwoInts second; };" +
                     "void main() {" +
                     "  FourInts x = FourInts(TwoInts(1, 2), TwoInts(3, 4));" +
                     "  TwoInts* p = &x.second;" +
                     "  *p = TwoInts(5, 6);" +
                     "  print(x.second.x);" +
                     "}",
                     5);
    }

    @Test
    public void testDereferenceInt() throws Exception {
        assertResult("void main() {" +
                     "  int x = 5;" +
                     "  int* p = &x;" +
                     "  print(*p);" +
                     "}",
                     5);
    }

    @Test
    public void testDereferenceStructure() throws Exception {
        assertResult("struct TwoInts { int x; int y; };" +
                     "void main() {" +
                     "  TwoInts x = TwoInts(1, 2);" +
                     "  TwoInts* p = &x;" +
                     "  print((*p).y);" +
                     "}",
                     2);
    }

    @Test
    public void testIfTrue() throws Exception {
        assertResult("void main() {" +
                     "  if (true) { print(1); } else { print(2); }" +
                     "}",
                     1);
    }

    @Test
    public void testIfFalse() throws Exception {
        assertResult("void main() {" +
                     "  if (false) { print(1); } else { print(2); }" +
                     "}",
                     2);
    }

    public static String NESTED_IF =
        "if (89 < x) { print(1); } else if (79 < x) { print(2); }" +
        " else if (69 < x) { print(3); } else if (59 < x) { print(4); }" +
        " else { print(5); }";

    @Test
    public void testNestedIfFirst() throws Exception {
        assertResult("void main() {" +
                     " int x = 90;" +
                     NESTED_IF +
                     "}",
                     1);
    }

    @Test
    public void testNestedIfSecond() throws Exception {
        assertResult("void main() {" +
                     " int x = 80;" +
                     NESTED_IF +
                     "}",
                     2);
    }

    @Test
    public void testNestedIfThird() throws Exception {
        assertResult("void main() {" +
                     " int x = 70;" +
                     NESTED_IF +
                     "}",
                     3);
    }

    @Test
    public void testNestedIfFourth() throws Exception {
        assertResult("void main() {" +
                     " int x = 60;" +
                     NESTED_IF +
                     "}",
                     4);
    }

    @Test
    public void testNestedIfFifth() throws Exception {
        assertResult("void main() {" +
                     " int x = 50;" +
                     NESTED_IF +
                     "}",
                     5);
    }

    @Test
    public void testIfComplexGuardTrue() throws Exception {
        assertResult("struct TwoInts { int x; int y; };" +
                     "void main() {" +
                     "  if (0 < TwoInts(1, 2).x) {" +
                     "    print(1);" +
                     "  } else {" +
                     "    print(2);" +
                     "  }" +
                     "}",
                     1);
    }
                     
    @Test
    public void testIfComplexGuardFalse() throws Exception {
        assertResult("struct TwoInts { int x; int y; };" +
                     "void main() {" +
                     "  if (TwoInts(1, 2).x < 0) {" +
                     "    print(1);" +
                     "  } else {" +
                     "    print(2);" +
                     "  }" +
                     "}",
                     2);
    }

    @Test
    public void testWhileInitiallyFalse() throws Exception {
        assertResult("void main() {" +
                     "  int x = 0;" +
                     "  while (x < 0) {" +
                     "    x = x + 1;" +
                     "  }" +
                     "  print(x);" +
                     "}",
                     0);
    }

    @Test
    public void testWhileInitiallyTrue() throws Exception {
        assertResult("void main() {" +
                     "  int x = 0;" +
                     "  while (x < 10) {" +
                     "    x = x + 1;" +
                     "  }" +
                     "  print(x);" +
                     "}",
                     10);
    }

    @Test
    public void testUnconditionalBreak() throws Exception {
        assertResult("void main() {" +
                     "  int x = 0;" +
                     "  while (x < 10) {" +
                     "    x = x + 1; break;" +
                     "  }" +
                     "  print(x);" +
                     "}",
                     1);
    }

    @Test
    public void testUnconditionalContinue() throws Exception {
        assertResult("void main() {" +
                     "  int x = 0;" +
                     "  int y = 0;" +
                     "  while (x < 10) {" +
                     "    x = x + 1;" +
                     "    continue;" +
                     "    y = y + 1;" +
                     "  }" +
                     "  print(x);" +
                     "  print(y);" +
                     "}",
                     10,
                     0);
    }

    @Test
    public void testConditionalBreakContinue() throws Exception {
        assertResult("void main() {" +
                     "  int x = 0;" +
                     "  while (true) {" +
                     "    x = x + 1;" +
                     "    if (10 < x) {" +
                     "      break;" +
                     "    } else {" +
                     "      continue;" +
                     "    }" +
                     "  }" +
                     "  print(x);" +
                     "}",
                     11);
    }

    @Test
    public void testIfScope() throws Exception {
        assertResult("void main() {" +
                     "  int x = 0;" +
                     "  if (true) {" +
                     "    int x = 1;" +
                     "  } else {" +
                     "    int x = 2;" +
                     "  }" +
                     "  print(x);" +
                     "}",
                     0);
    }

    @Test
    public void testWhileScope() throws Exception {
        assertResult("void main() {" +
                     "  int x = 0;" +
                     "  while (true) {" +
                     "    int x = 1;" +
                     "    break;" +
                     "  }" +
                     "  print(x);" +
                     "}",
                     0);
    }

    @Test
    public void testNestedWhile() throws Exception {
        assertResult("void main() {" +
                     "  int result = 0;" +
                     "  int x = 0;" +
                     "  while (x < 5) {" +
                     "    int y = 0;" +
                     "    while (y < 5) {" +
                     "      result = result + 1;" +
                     "      y = y + 1;" +
                     "    }" +
                     "    x = x + 1;" +
                     "  }" +
                     "  print(x);" +
                     "  print(result);" +
                     "}",
                     5,
                     25);
    }
    // ---END TESTS FOR STATEMENTS---

    // ---BEGIN TESTS FOR FUNCTIONS---
    @Test
    public void testPrintConstantExplicitReturn() throws Exception {
        assertResult("void main() { print(1); return; }",
                     1);
    }

    @Test
    public void testPrintConstantImplicitReturn() throws Exception {
        assertResult("void main() { print(1); }",
                     1);
    }

    @Test
    public void testCallFunctionReturnsConstantInt() throws Exception {
        assertResult("int foo() { return 1; }" +
                     "void main() { print(foo()); }",
                     1);
    }

    @Test
    public void testCallFunctionAddsParams() throws Exception {
        assertResult("int foo(int x, int y) { return x + y; }" +
                     "void main() { print(foo(1, 2)); }",
                     3);
    }

    @Test
    public void testReturnStructureConstantGetFirst() throws Exception {
        assertResult("struct TwoInts { int x; int y; };" +
                     "TwoInts foo() { return TwoInts(1, 2); }" +
                     "void main() { print(foo().x); }",
                     1);
    }

    @Test
    public void testReturnStructureConstantGetSecond() throws Exception {
        assertResult("struct TwoInts { int x; int y; };" +
                     "TwoInts foo() { return TwoInts(1, 2); }" +
                     "void main() { print(foo().y); }",
                     2);
    }

    @Test
    public void testReturnStructureParamsGetFirst() throws Exception {
        assertResult("struct TwoInts { int x; int y; };" +
                     "TwoInts foo(int a, int b) { return TwoInts(a, b); }" +
                     "void main() { print(foo(1, 2).x); }",
                     1);
    }

    @Test
    public void testReturnStructureParamsGetSecond() throws Exception {
        assertResult("struct TwoInts { int x; int y; };" +
                     "TwoInts foo(int a, int b) { return TwoInts(a, b); }" +
                     "void main() { print(foo(1, 2).y); }",
                     2);
    }

    @Test
    public void testCanTakeStructureGetFirst() throws Exception {
        assertResult("struct TwoInts { int x; int y; };" +
                     "void foo(TwoInts s) { print(s.x); }" +
                     "void main() { foo(TwoInts(1, 2)); }",
                     1);
    }

    @Test
    public void testCanTakeStructureGetSecond() throws Exception {
        assertResult("struct TwoInts { int x; int y; };" +
                     "void foo(TwoInts s) { print(s.y); }" +
                     "void main() { foo(TwoInts(1, 2)); }",
                     2);
    }

    @Test
    public void testCanTakeMultipleStructures() throws Exception {
        assertResult("struct TwoInts { int x; int y; };" +
                     "struct FourInts { TwoInts first; TwoInts second; };" +
                     "FourInts foo(TwoInts x, TwoInts y) {" +
                     "  return FourInts(x, y);" +
                     "}" +
                     "void main() {" +
                     "  FourInts f = foo(TwoInts(1, 2), TwoInts(3, 4));" +
                     "  print(f.first.x);" +
                     "  print(f.first.y);" +
                     "  print(f.second.x);" +
                     "  print(f.second.y);" +
                     "}",
                     1,
                     2,
                     3,
                     4);
    }

    @Test
    public void testFibonacci() throws Exception {
        assertResult("int fib(int x) {" +
                     "  if (x == 0) {" +
                     "    return 0;" +
                     "  } else if (x == 1) {" +
                     "    return 1;" +
                     "  } else {" +
                     "    return fib(x - 1) + fib(x - 2);" +
                     "  }" +
                     "}" +
                     "void main() { print(fib(7)); }",
                     13);
    }

    @Test
    public void testIndirectCallSingleFunction() throws Exception {
        assertResult("void printOne() { print(1); }" +
                     "void main() {" +
                     "   () => void f = &printOne;" +
                     "   f();" +
                     "   print(2);" +
                     "}",
                     1,
                     2);
    }

    @Test
    public void testIndirectCallReturnsInt() throws Exception {
        assertResult("int returnsOne() { return 1; }" +
                     "void main() {" +
                     "   () => int f = &returnsOne;" +
                     "   print(f());" +
                     "}",
                     1);
    }

    @Test
    public void testIndirectCallReturnsStruct() throws Exception {
        assertResult("struct TwoInts { int x; int y; };" +
                     "TwoInts returnsStruct() { return TwoInts(1, 2); }" +
                     "void main() {" +
                     "   () => TwoInts f = &returnsStruct;" +
                     "   TwoInts pair = f();" +
                     "   print(pair.x);" +
                     "   print(pair.y);" +
                     "}",
                     1,
                     2);
    }

    @Test
    public void testIndirectCallReturnsStructTakesParams() throws Exception {
        assertResult("struct TwoInts { int x; int y; };" +
                     "TwoInts returnsStruct(int x, int y) { return TwoInts(x, y); }" +
                     "void main() {" +
                     "   (int, int) => TwoInts f = &returnsStruct;" +
                     "   TwoInts pair = f(1, 2);" +
                     "   print(pair.x);" +
                     "   print(pair.y);" +
                     "}",
                     1,
                     2);
    }

                         
    @Test
    public void testIndirectCallTwoFunctions() throws Exception {
        assertResult("int add(int x, int y) { return x + y; }" +
                     "int sub(int x, int y) { return x - y; }" +
                     "void main() {" +
                     "  (int, int) => int f = &add;" +
                     "  print(f(1, 2));" +
                     "  f = &sub;" +
                     "  print(f(5, 1));" +
                     "}",
                     3,
                     4);
    }

    @Test
    public void testStructLayout() throws Exception {
        assertResult("struct TwoInts { int x; int y; };" +
                     "struct FourInts { TwoInts first; TwoInts second; };" +
                     "void main() {" +
                     "  FourInts f = FourInts(TwoInts(1, 2), TwoInts(3, 4));" +
                     "  FourInts* fp = &f;" +
                     "  TwoInts* p = (TwoInts*)fp;" +
                     "  print((*p).x);" +
                     "  print((*p).y);" +
                     "}",
                     3,
                     4);
    }
    // ---END TESTS FOR FUNCTIONS---
}
