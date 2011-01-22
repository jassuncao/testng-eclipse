package org.testng.eclipse.wizards;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.internal.corext.util.JavaConventionsUtil;
import org.eclipse.jdt.ui.IJavaElementSearchConstants;
import org.eclipse.jdt.ui.JavaElementLabels;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.wizards.NewTypeWizardPage;
import org.eclipse.jface.dialogs.IDialogPage;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.SelectionDialog;
import org.testng.eclipse.TestNGPlugin;
import org.testng.eclipse.util.ResourceUtil;
import org.testng.eclipse.util.SWTUtil;
import org.testng.eclipse.util.Utils;

/**
 * Generate a new TestNG class and optionally, the corresponding XML suite file.
 */
public class NewTestNGClassWizardPage extends NewTypeWizardPage  {
  private final static String PAGE_NAME= "NewTestNGClassWizardPage"; //$NON-NLS-1$
  
  /** Field ID of the class under test field. */
  public final static String CLASS_UNDER_TEST= PAGE_NAME + ".classundertest";
  private final static String TEST_SUFFIX= "Test";
  
  private Text m_xmlFilePath;

  private Map<String, Button> m_annotations = new HashMap<String, Button>();  

  private Text m_classUnderTestControl;
  private IType m_classUnderTest;
  private String m_classUnderTestText;
  private IStatus m_classUnderTestStatus;
  private Button m_classUnderTestButton;
  private final TestNGMethodWizardPage m_methodsPage;
  
  public static final String[] ANNOTATIONS = new String[] {
    "BeforeMethod", "AfterMethod", "DataProvider",
    "BeforeClass", "AfterClass", "",
    "BeforeTest",  "AfterTest", "",
    "BeforeSuite", "AfterSuite", ""
  };

  public NewTestNGClassWizardPage(TestNGMethodWizardPage methodPage) {
    super(true, ResourceUtil.getString("NewTestNGClassWizardPage.title"));
    setTitle(ResourceUtil.getString("NewTestNGClassWizardPage.title"));
    setDescription(ResourceUtil.getString("NewTestNGClassWizardPage.description"));
    m_classUnderTestStatus= TestNGStatus.createOK();
    m_classUnderTestText= "";
    m_methodsPage = methodPage;
  }

  /**
   * @see IDialogPage#createControl(Composite)
   */
  public void createControl(Composite parent) {
    Composite container = new Composite(parent, SWT.NULL);
    GridLayout layout = new GridLayout();
    layout.numColumns = 1;
    container.setLayout(layout);
    createTop(container);
    createBottom(container);
    setControl(container);
  }

  private void createTop(Composite parent) {
    int nColumns = 4;
    Composite composite = SWTUtil.createGridContainer(parent, nColumns);
    createContainerControls(composite, nColumns);
    createPackageControls(composite, nColumns);
    createSeparator(composite, nColumns);
    createTypeNameControls(composite, nColumns);
    createSeparator(composite, nColumns);
    createClassUnderTestControls(composite, nColumns);
  }
  
  /**
   * Creates the controls for the 'class under test' field. Expects a <code>GridLayout</code> with
   * at least 3 columns.
   *
   * @param composite the parent composite
   * @param nColumns number of columns to span
   */
  protected void createClassUnderTestControls(Composite composite, int nColumns) {
    Label classUnderTestLabel= new Label(composite, SWT.LEFT | SWT.WRAP);
    classUnderTestLabel.setFont(composite.getFont());
    classUnderTestLabel.setText("C&lass under test:");
    classUnderTestLabel.setLayoutData(new GridData());

    m_classUnderTestControl= new Text(composite, SWT.SINGLE | SWT.BORDER);
    m_classUnderTestControl.setEnabled(true);
    m_classUnderTestControl.setFont(composite.getFont());
    m_classUnderTestControl.setText(m_classUnderTestText);
    m_classUnderTestControl.addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        internalSetClassUnderText(((Text) e.widget).getText());
      }
    });
    GridData gd= new GridData();
    gd.horizontalAlignment= GridData.FILL;
    gd.grabExcessHorizontalSpace= true;
    gd.horizontalSpan= nColumns - 2;
    m_classUnderTestControl.setLayoutData(gd);

    m_classUnderTestButton= new Button(composite, SWT.PUSH);
    m_classUnderTestButton.setText("B&rowse...");
    m_classUnderTestButton.setEnabled(getPackageFragmentRoot() != null);
    m_classUnderTestButton.addSelectionListener(new SelectionListener() {
      public void widgetDefaultSelected(SelectionEvent e) {
        classToTestButtonPressed();
      }
      public void widgetSelected(SelectionEvent e) {
        classToTestButtonPressed();
      }
    });
    gd= new GridData();
    gd.horizontalAlignment= GridData.FILL;
    gd.grabExcessHorizontalSpace= false;
    gd.horizontalSpan= 1;
    gd.widthHint = SWTUtil.getButtonWidthHint(m_classUnderTestButton);
    m_classUnderTestButton.setLayoutData(gd);
  }
  
  private void classToTestButtonPressed() {
    IType type= chooseClassToTestType();
    if (type != null) {
      String classUnderTest = type.getFullyQualifiedName('.');
      setClassUnderTest(classUnderTest);
      setTypeName(Signature.getSimpleName(classUnderTest)+TEST_SUFFIX, true);
    }
  }
    
  protected IStatus classUnderTestChanged() {    
    m_classUnderTest= null;

    IPackageFragmentRoot root= getPackageFragmentRoot();
    if (root == null) {
      return TestNGStatus.createOK();
    }

    String classToTestName= getClassUnderTestText();
    if (classToTestName.length() == 0) {
      return TestNGStatus.createOK();
    }

    IStatus val= JavaConventionsUtil.validateJavaTypeName(classToTestName, root);
    if (val.getSeverity() == IStatus.ERROR) {
      return TestNGStatus.createError("Class under test is not valid.");      
    }

    IPackageFragment pack= getPackageFragment(); // can be null
    try {
      IType type= resolveClassNameToType(root.getJavaProject(), pack, classToTestName);
      if (type == null) {
        return TestNGStatus.createError("Class under test does not exist in current project.");
      }
      if (type.isInterface()) {
        return TestNGStatus.createWarning(MessageFormat.format("Warning: Class under test ''{0}'' is an interface.", JavaElementLabels.ALL_DEFAULT));
      }

      if (pack != null && !Utils.isVisible(type, pack)) {
        return TestNGStatus.createWarning(MessageFormat.format("''{0}'' is not visible.", JavaElementLabels.ALL_DEFAULT));        
      }
      m_classUnderTest= type;
      m_methodsPage.setClassUnderTest(m_classUnderTest);
    } catch (JavaModelException e) {
      return TestNGStatus.createError("Class under test is not valid.");
    }
    return TestNGStatus.createOK();
  }
  
  private IType resolveClassNameToType(IJavaProject jproject, IPackageFragment pack, String classToTestName) throws JavaModelException {
    if (!jproject.exists()) {
      return null;
    }

    IType type= jproject.findType(classToTestName);

    // search in current package
    if (type == null && pack != null && !pack.isDefaultPackage()) {
      type= jproject.findType(pack.getElementName(), classToTestName);
    }

    // search in java.lang
    if (type == null) {
      type= jproject.findType("java.lang", classToTestName); //$NON-NLS-1$
    }
    return type;
  }
  
  /**
   * Returns the content of the class to test text field.
   *
   * @return the name of the class to test
   */
  public String getClassUnderTestText() {
    return m_classUnderTestText;
  }

  /**
   * Returns the class to be tested.
   *
   *  @return the class under test or <code>null</code> if the entered values are not valid
   */
  public IType getClassUnderTest() {
    return m_classUnderTest;
  }

  /**
   * Sets the name of the class under test.
   *
   * @param name The name to set
   */
  public void setClassUnderTest(String name) {
    if (m_classUnderTestControl != null && !m_classUnderTestControl.isDisposed()) {
      m_classUnderTestControl.setText(name);           
    }
    internalSetClassUnderText(name);
  }

  private void internalSetClassUnderText(String name) {
    m_classUnderTestText= name;
    m_classUnderTestStatus= classUnderTestChanged();
    handleFieldChanged(CLASS_UNDER_TEST);
  }  

  private IType chooseClassToTestType() {
    IPackageFragmentRoot root= getPackageFragmentRoot();
    if (root == null)
      return null;

    IJavaElement[] elements= new IJavaElement[] { root.getJavaProject() };
    IJavaSearchScope scope= SearchEngine.createJavaSearchScope(elements);

    try {
      SelectionDialog dialog= JavaUI.createTypeDialog(getShell(), getWizard().getContainer(), scope, IJavaElementSearchConstants.CONSIDER_CLASSES_AND_ENUMS, false, getClassUnderTestText());
      dialog.setTitle("Class Under Test");
      dialog.setMessage("Test stubs will be generated for class:");
      if (dialog.open() == Window.OK) {
        Object[] resultArray= dialog.getResult();
        if (resultArray != null && resultArray.length > 0)
          return (IType) resultArray[0];
      }
    } catch (JavaModelException e) {
      TestNGPlugin.log(e);
    }
    return null;
  }

  private void createBottom(Composite parent) {
    //
    // Annotations
    //
    {
      Group g = new Group(parent, SWT.SHADOW_ETCHED_OUT);
      g.setText("Annotations");
      GridData gd = new GridData(GridData.FILL_HORIZONTAL);
      g.setLayoutData(gd);

      GridLayout layout = new GridLayout();
      g.setLayout(layout);
      layout.numColumns = 3;

      for (String label : ANNOTATIONS) {
        if ("".equals(label)) {
          new Label(g, SWT.NONE);
        } else {
          Button b = new Button(g, "".equals(label) ? SWT.None : SWT.CHECK);
          m_annotations.put(label, b);
          b.setText("@" + label);
        }
      }
    }

    //
    // XML suite file
    //
    {
      Composite container = SWTUtil.createGridContainer(parent, 2);

      //
      // Label
      //
      Label label = new Label(container, SWT.NULL);
      label.setText(ResourceUtil.getString("TestNG.newClass.suitePath"));

      //
      // Text widget
      //
      m_xmlFilePath = new Text(container, SWT.SINGLE | SWT.BORDER);
      GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL);
      gd.grabExcessHorizontalSpace = true;
      m_xmlFilePath.setLayoutData(gd);
    }
  }
  
  public void init(IStructuredSelection selection) {
    IJavaElement element= getInitialJavaElement(selection);
    initContainerPage(element);
    initTypePage(element);
 // put default class to test
    if (element != null) {
      IType classToTest= null;
      // evaluate the enclosing type
      IType typeInCompUnit= (IType) element.getAncestor(IJavaElement.TYPE);
      if (typeInCompUnit != null) {
        if (typeInCompUnit.getCompilationUnit() != null) {
          classToTest= typeInCompUnit;
        }
      } else {
        ICompilationUnit cu= (ICompilationUnit) element.getAncestor(IJavaElement.COMPILATION_UNIT);
        if (cu != null)
          classToTest= cu.findPrimaryType();
        else {
          if (element instanceof IClassFile) {
            try {
              IClassFile cf= (IClassFile) element;
              if (cf.isStructureKnown())
                classToTest= cf.getType();
            } catch(JavaModelException e) {
              TestNGPlugin.log(e);
            }
          }
        }
      }
      if (classToTest != null) {
        String classUnderTest=classToTest.getFullyQualifiedName('.');
        setClassUnderTest(classUnderTest);          
        setTypeName(Signature.getSimpleName(classUnderTest)+TEST_SUFFIX, true);
      }
    }    
    updateStatus(getStatusList());
  }
  
  protected IStatus[] getStatusList() {
    return new IStatus[] {  
        fContainerStatus,
        fPackageStatus,
        fTypeNameStatus,
        m_classUnderTestStatus
    };
  }
  
  protected void handleFieldChanged(String fieldName) {
    super.handleFieldChanged(fieldName);
    if (fieldName.equals(CONTAINER)) {
      m_classUnderTestStatus= classUnderTestChanged();
      if (m_classUnderTestButton != null && !m_classUnderTestButton.isDisposed()) {
        m_classUnderTestButton.setEnabled(getPackageFragmentRoot() != null);
      }     
    } 
    updateStatus(getStatusList());
  } 

  public String getSourceFolder() {
    return getPackageFragmentRootText();    
  }

  public String getXmlFile() {
    return m_xmlFilePath.getText();
  }

  public String getPackageName() {
    return getPackageText();    
  }

  public String getClassName() {
    return getTypeName();
  }

  public boolean containsAnnotation(String annotation) {
    Button b = m_annotations.get(annotation);
    return b.getSelection();
  }
  
}