package tangentz;

import java.util.*;
import java.io.*;
import com.topcoder.client.contestant.ProblemComponentModel;
import com.topcoder.shared.language.Language;
import com.topcoder.shared.problem.*;


/**
 * @author TangentZ
 *
 * This tester class is for C++ only.  It is based on PopsProcessor which is written for Java.
 * It reads in all the given examples for a problem and generates the equivalent C++ code
 * to test all the cases.  The accumulated running time is 8 seconds, but it is easy to
 * selectively run a specific case only.
 *
 * This tester will define three tags that can be embedded within PopsEdit/FileEdit code template:
 *  $WRITERCODE$ - place holder for writer code - will be blank if none found
 *  $PROBLEM$ - place holder for problem description as plain text
 *  // $RUNTEST$ - place holder for where to put the code that starts the test
 *  $TESTCODE$ - place holder for where to put the test code
 */
public class TZTester
{
    // Map used to store my tags
    private HashMap<String,String> m_Tags = new HashMap<String,String>();

    // Constants
    private static final String k_WRITERCODE = "$WRITERCODE$";
    private static final String k_PROBLEM    = "$PROBLEM$";
    private static final String k_RUNTEST    = "$RUNTEST$";
    private static final String k_TESTCODE   = "$TESTCODE$";
    private static final String k_VERSION    = "\n// Powered by TZTester 1.01 [25-Feb-2003] : <cafelier&naoya_t>-custom";

    // Cut tags
    private static final String k_BEGINCUT   = "// BEGIN CUT HERE";
    private static final String k_ENDCUT     = "// END CUT HERE";

    /**
     * PreProcess the source code
     * 
     * First determines if it is saved code, writer code, or nothing and stores it in $WRITERCODE$ tag
     * Secondly builds a main method with default test cases
     */
    public String preProcess(String Source, ProblemComponentModel Problem, Language Lang, Renderer Render)
    {
        // Set defaults for the tags in case we exit out early
        m_Tags.put(k_WRITERCODE, "");
        m_Tags.put(k_PROBLEM,    "");
        m_Tags.put(k_RUNTEST,    "// *** WARNING *** $RUNTEST$ is not supported by this customized TZTester.");
        m_Tags.put(k_TESTCODE,   "");

        // If there is source and the source is NOT equal to the default solution, return it
        if( Source.length()>0 && !Source.equals(Problem.getDefaultSolution()) )
            return Source;

        // Check to see if the component has any signature 
        if( !Problem.hasSignature() )
            {
                m_Tags.put(k_TESTCODE, "// *** WARNING *** Problem has no signature defined for it");
                return "";
            }
        
        // Get the test cases
        TestCase[] TestCases = Problem.getTestCases();

        // Check to see if test cases are defined
        if( TestCases==null || TestCases.length==0 )
            {
                m_Tags.put(k_TESTCODE, "// *** WARNING *** No test cases defined for this problem");
                return "";
            }

        // Re-initialize the tags
        m_Tags.clear();
        m_Tags.put(k_WRITERCODE, Problem.getDefaultSolution());
        try { m_Tags.put(k_PROBLEM, Render.toHTML(Lang)); } catch (Exception Ex) { }

        // Generate header file if not exists
        generate_header_file(Problem, Lang);

        // Generate the test cases
        generate_test_code(Problem, Lang);
        
        return "";
    }

    /**
     * This method will cut the test methods above out
     */
    public String postProcess(String Source, Language Lang)
    {
        // Insert a version string
        return Source + k_VERSION;
    }

    /**
     * This method will return my tags.  This method is ALWAYS called after preProcess()
     * 
     * @return a map of my tags
     */
    public Map getUserDefinedTags()
    {
        return m_Tags;
    }

    private static String header_file_name(ProblemComponentModel Problem){
        return Problem.getClassName()+"_"+Problem.getMethodName()+".h";
    }

    private void generate_header_file(ProblemComponentModel Problem, Language Lang) {

        String userHome = System.getProperty("user.home");
        File file = new File(userHome+"/.topcoder");
        if(!file.exists())
            file.mkdir();
        File hfile = new File(userHome+"/.topcoder/"+header_file_name(Problem));

        DataType[] ParamTypes = Problem.getParamTypes();
        DataType ReturnType   = Problem.getReturnType();
        TestCase[] Cases      = Problem.getTestCases();
        
        try {
            FileWriter fw = new FileWriter(hfile);
            fw.write(new String(build_header_code(ParamTypes, ReturnType, Cases, Problem, Lang)));
            fw.close();
        }
        catch(Exception e){
            e.printStackTrace();
        }

    }

    private static StringBuilder build_header_code(DataType[] ParamTypes, DataType ReturnType, TestCase[] Cases,ProblemComponentModel Problem, Language Lang) {

        StringBuilder Code = new StringBuilder();

        // <<thanks to naoya_t>> Generate the timer function
        Code.append("#include <ctime>\n");
        Code.append("#include <sstream>\n");  // added on July 2, 2011 by sabottenda
        Code.append("#include <fstream>\n");  // added on June 11, 2011 by sabottenda
        Code.append("double start_time; string timer()\n");
        Code.append(" { ostringstream os; os << \" (\" << int((clock()-start_time)/CLOCKS_PER_SEC*1000) << \" msec)\"; return os.str(); }\n");

        // Generate the vector output function
        Code.append("template<typename T> ostream& operator<<(ostream& os, const vector<T>& v)\n");
        Code.append(" { os << \"{ \";\n");
        Code.append("   for(typename vector<T>::const_iterator it=v.begin(); it!=v.end(); ++it)\n");
        Code.append("   os << \'\\\"\' << *it << \'\\\"\' << (it+1==v.end() ? \"\" : \", \"); os << \" }\"; return os; }\n");

        // Generate the verification function
        Code.append(generate_verification_code(Cases[0], ParamTypes, ReturnType, Problem, Lang));

        // Generate the memory usage notification function (by sabottenda)
        Code.append(generate_memory_usage_notification());

        // <<modified by cafelier>> : new test code template
        Code.append("#define CASE(N) if (N==runno_ || (runno_<0 && N+1>=-runno_)) {int caseno=N; start_time=clock();\n");
        Code.append("#define RUN_TEST()  ");
        Code.append(new String(generate_verifier_call(ParamTypes, ReturnType, Cases[0], Problem, Lang)));
        Code.append("}\n");

        return Code;
    }

    /**
     * This method will generate the code for the test cases.
     */
    private void generate_test_code(ProblemComponentModel Problem, Language Lang)
    {
        DataType[] ParamTypes = Problem.getParamTypes();
        DataType ReturnType   = Problem.getReturnType();
        TestCase[] Cases      = Problem.getTestCases();
        
        // Replace $TESTCODE$
        m_Tags.put(k_TESTCODE, new String(build_test_code(ParamTypes, ReturnType, Cases, Problem, Lang)));
    }

    private void hoge(){}

    private static StringBuilder build_test_code(DataType[] ParamTypes, DataType ReturnType, TestCase[] Cases, ProblemComponentModel Problem, Language Lang) {
        
        StringBuilder Code = new StringBuilder();
        
        Code.append("#include \""+System.getProperty("user.home")+"/.topcoder/"+header_file_name(Problem)+"\"\n");
        Code.append("#include <initializer_list>\n");
        Code.append("int main(int argc, char **argv){\n");
        Code.append("    bool verbose_ = false;\n");
        Code.append("    int runno_ = -1;\n");
        Code.append("    if (argc >= 2) if(!strcmp(argv[1], \"-v\")) verbose_ = true;\n");
        Code.append("    if (argc == 2 && !verbose_) runno_ = atoi(argv[1]);\n");
        Code.append("    else if (argc == 3 && verbose_) runno_ = atoi(argv[2]);\n\n");
        
        // Generate the individual test cases
        for(int i=0; i<Cases.length; ++i)
            Code.append(new String(generate_test_case(i, ParamTypes, ReturnType, Cases[i], Problem, Lang)));

        // Generate the memory usage notification by sabottenda on June 11, 2011
        Code.append("    notify_memory_usage();\n");

        Code.append("\n}\n");

        // Insert the cut tags
        Code.insert(0, k_BEGINCUT+"\n");
        Code.append(k_ENDCUT);

        return Code;
    }

    /**
     * This method will generate the code for verifying test cases.
     */
    private static StringBuilder generate_verification_code(TestCase Case, DataType[] ParamTypes, DataType ReturnType, ProblemComponentModel Problem, Language Lang)
    {
        StringBuilder Code = new StringBuilder();

        String TypeString = ReturnType.getDescriptor(Lang);
        String[] Inputs = Case.getInput();

        // <<modified by cafelier>> : new test code template
        Code.append("void verify_case(const int caseno");
        for(int i=0; i<Inputs.length; ++i)
            //Code.append(", const " + ParamTypes[i].getDescriptor(Lang) + "&" + "\n");
            Code.append(", const " + ParamTypes[i].getDescriptor(Lang) + "&" + Problem.getParamNames()[i]);
        Code.append(", const " + TypeString + "& Expected, bool verbose = false) {\n");
        //Code.append(", const " + TypeString + "& Expected, const " + TypeString + "& Received, bool verbose = false) {\n");

        Code.append("  " + TypeString + " Received = " + Problem.getClassName() + "()." + Problem.getMethodName() + "(");
        Code.append(Problem.getParamNames()[0]);
        for(int i=1; i<Inputs.length; ++i)
            Code.append(", " + Problem.getParamNames()[i]);
        Code.append(");\n");
            
        
        Code.append("  cerr << \"Test Case #\" << caseno << \"...\";\n"); // added on June 11, 2011 by sabottenda
        // Print "PASSED" or "FAILED" based on the result
        // <<modified by naoya_t>> : double precision
        if (TypeString.equals("double")) {
            Code.append(" double diff = Expected - Received; if (diff < 0) diff = -diff;");
            Code.append(" bool ok = (diff < 1e-9);\n");
        } else if (TypeString.equals("vector <double>")) { // vector<double> support added on Feb 19, 2010 by naoya_t
            Code.append("  bool passed = Received.size()==Expected.size(); ");
            Code.append("  if (passed) for (int i=0,c=Received.size(); i<c; i++) { ");
            Code.append("  double diff = Expected[i] - Received[i]; if (diff < 0) diff = -diff; ");
            Code.append("  if (diff >= 1e-9) { passed = false; break; }} ");
            Code.append("  if (passed) cerr << \"PASSED\" << timer() << endl; ");
        } else {
            Code.append("  bool ok = (Expected == Received);\n");
        }
        Code.append("  if(ok) cerr << \"PASSED\" << timer() << endl; ");
        Code.append("  else { cerr << \"FAILED\" << timer() << endl;\n");

        if (ReturnType.getDimension() == 0)
            {
                for(int i=0; i<Inputs.length; ++i)
                    Code.append("  if (verbose) cerr << \"\\t" + Problem.getParamNames()[i] + ": \" << " + Problem.getParamNames()[i]  + "<< endl;\n");
                Code.append("  cerr << \"\\to: \\\"\" << Expected << \'\\\"\' << endl ");
                Code.append("<< \"\\tx: \\\"\" << Received << \'\\\"\' << endl; }");
            }
        else
            {
                for(int i=0; i<Inputs.length; ++i)
                    Code.append("  if (verbose) cerr << \"\\t" + Problem.getParamNames()[i] + ": \" << " + Problem.getParamNames()[i]  + "<< endl;\n");
                Code.append("  cerr << \"\\to: \" << Expected << endl ");
                Code.append("<< \"\\tx: \" << Received << endl; }");
            }

        Code.append(" }\n");
        return Code;
    }

    /**
     * This method will generate the code for verifying test cases.
     */
    private static StringBuilder generate_memory_usage_notification()
    {
        StringBuilder Code = new StringBuilder();
        Code.append("void notify_memory_usage(){\n");
        Code.append("#ifndef _WIN32\n");
        Code.append("  std::ifstream ifs(\"/proc/self/status\",std::ios_base::in);\n");
        Code.append("  std::string str;\n");
        Code.append("  for(;;){std::getline(ifs, str);\n");
        Code.append("    if(str.find(\"VmPeak\") != std::string::npos){cout << str << \" (< 64MB)\" << endl;}\n");
        Code.append("    if(str.find(\"VmStk\") != std::string::npos){cout << str << \" (< 8MB)\" << endl;break;} }\n");
        Code.append("#endif\n");
        Code.append("}\n");
        return Code;
    }

    /**
     * This method will generate the code for one test case.
     */
    private static StringBuilder generate_test_case(int Index, DataType[] ParamTypes, DataType ReturnType, TestCase Case, ProblemComponentModel Problem, Language Lang)
    {
        String[] Inputs = Case.getInput();
        String Output = Case.getOutput();
        String Desc = ReturnType.getDescription();

        StringBuilder Code = new StringBuilder();

        /*
         * Generate code for setting up individual test cases
         * and calling the method with these parameters.
         */

        // <<modified by cafelier>> : new test code template
        Code.append("    CASE("+Index+"){\n");

        // Generate each input variable separately
        for (int i = 0; i < Inputs.length; ++i) {
            Code.append("        ");
            Code.append(new String(generate_parameter(i, ParamTypes[i], Inputs[i], Problem, Lang)));
            Code.append("\n");
        }

        // Generate the output variable as the last variable
        Code.append("        ");
        Code.append(new String(generate_parameter(-1, ReturnType, Output, Problem, Lang)));
        Code.append("\n");
        Code.append("        RUN_TEST();\n");
        Code.append("    }\n");

        return Code;
    }

    private static StringBuilder generate_verifier_call(DataType[] ParamTypes, DataType ReturnType, TestCase Case, ProblemComponentModel Problem, Language Lang)
    {
        StringBuilder Code = new StringBuilder();

        String[] Inputs = Case.getInput();
        String Output = Case.getOutput();
        String Desc = ReturnType.getDescription();

        Code.append("verify_case(caseno, ");
        //+ Problem.getClassName() + "()." + Problem.getMethodName() + "(");
        for(int i=0; i<Inputs.length; ++i)
            {
                Code.append( Problem.getParamNames()[i] );
                if( i < Inputs.length-1 )
                    Code.append(", ");
            }
        //Code.append("));");
        Code.append(", _, verbose_);");
        return Code;
    }

    /**
     * This method will generate the required parameter as a unique variable.
     */
    private static StringBuilder generate_parameter(int Index, DataType ParamType, String Input, ProblemComponentModel Problem, Language Lang)
    {

        StringBuilder Code = new StringBuilder();

        // <<modified by cafelier>> : named parameters
        String Name = (Index==-1 ? "_" : Problem.getParamNames()[Index]);

        String Desc = ParamType.getBaseName();

        if (ParamType.getDimension() == 0)
            {
                // Just a scalar value, simply initialize it at declaration (long integers need an 'L' tagged on)
                if (Desc.equals("long") || Desc.equals("Long"))
                    Code.append(ParamType.getDescriptor(Lang) + " " + Name + " = " + Input + "LL;");
                else
                    Code.append(ParamType.getDescriptor(Lang) + " " + Name + " = " + Input + ";");
            }
        else
            {
                // <<modified by cafelier>> : empty array
                if( Input.matches("^[\\s\\{\\}]*$") )
                    Code.append(ParamType.getDescriptor(Lang) + " " + Name + "; ");
                else {
                    //c++0x feature
                    Input = Input.replace('\n', ' ');
                    Code.append(ParamType.getDescriptor(Lang) + " " + Name + " = " + Input + ";\n");
                }
            }

        return Code;
    }
}
