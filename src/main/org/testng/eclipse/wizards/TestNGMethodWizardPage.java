package org.testng.eclipse.wizards;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.dialogs.ContainerCheckedTreeViewer;
import org.testng.eclipse.TestNGPlugin;
import org.testng.eclipse.collections.Lists;
import org.testng.eclipse.util.ResourceUtil;

/**
 * A wizard page that displays the list of public methods on the currently selected class
 * so that the user can select or deselect them before creating a new test class.
 *
 * @author Cedric Beust <cedric@beust.com>
 */
public class TestNGMethodWizardPage extends WizardPage {
  
  private ContainerCheckedTreeViewer m_methodsTree;
  private IType m_classUnderTest;

  protected TestNGMethodWizardPage() {
    super(ResourceUtil.getString("NewTestNGClassWizardPage.title"));
    setTitle(ResourceUtil.getString("NewTestNGClassWizardPage.title"));
    setDescription(ResourceUtil.getString("TestNGMethodWizardPage.description"));   
  }
  
  public void setClassUnderTest(IType classUnderTest) {
    m_classUnderTest = classUnderTest;
  }

  public void createControl(Composite parent) {
    Composite container = new Composite(parent, SWT.NULL);

    {
      GridLayout layout = new GridLayout();
      layout.numColumns = 2;
      container.setLayout(layout);
    }

    {
      m_methodsTree= new ContainerCheckedTreeViewer(container, SWT.BORDER);
      GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
      gd.verticalSpan = 2;     
      m_methodsTree.getTree().setLayoutData(gd);
      m_methodsTree.setLabelProvider(new JavaElementLabelProvider());
      m_methodsTree.setAutoExpandLevel(2);
      m_methodsTree.addCheckStateListener(new ICheckStateListener() {
        public void checkStateChanged(CheckStateChangedEvent event) {
          doCheckedStateChanged();
        }
      });
      m_methodsTree.addFilter(new ViewerFilter() {        
        @Override
        public boolean select(Viewer viewer, Object parentElement, Object element) {
          if (element instanceof IMethod) {
            IMethod method = (IMethod) element;
            return !method.getElementName().equals("<clinit>"); //$NON-NLS-1$
          }
          return true;
        }
      });
    }

    {
      Composite cb = new Composite(container, SWT.NULL);
      GridLayout layout = new GridLayout();
      cb.setLayout(layout);

      Button selectAll = new Button(cb, SWT.NONE);
      selectAll.setText("Select all");
      selectAll.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, true));
      selectAll.addSelectionListener(new SelectionAdapter() {        
        public void widgetSelected(SelectionEvent e) {
          m_methodsTree.setCheckedElements((Object[]) m_methodsTree.getInput());
          doCheckedStateChanged();
        }              
      });      
  
      Button deselectAll = new Button(cb, SWT.NONE);
      deselectAll.setText("Deselect all");
      deselectAll.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, true));
      deselectAll.addSelectionListener(new SelectionAdapter() {        
        public void widgetSelected(SelectionEvent e) {
          m_methodsTree.setCheckedElements(new Object[0]);
          doCheckedStateChanged();
        }              
      });  
    }

    setControl(container);
  }
  
  public void setVisible(boolean visible) {
    super.setVisible(visible);
    if (visible) {
      if (m_classUnderTest == null) {
        return;
      }
      ArrayList<IType> types= null;
      try {
        ITypeHierarchy hierarchy= m_classUnderTest.newSupertypeHierarchy(null);
        IType[] superTypes;
        if (m_classUnderTest.isClass())
          superTypes= hierarchy.getAllSuperclasses(m_classUnderTest);
        else if (m_classUnderTest.isInterface())
          superTypes= hierarchy.getAllSuperInterfaces(m_classUnderTest);
        else
          superTypes= new IType[0];
        types= new ArrayList<IType>(superTypes.length+1);
        types.add(m_classUnderTest);
        types.addAll(Arrays.asList(superTypes));
      } catch(JavaModelException e) {
        TestNGPlugin.log(e);
      }
      if (types == null)
        types= new ArrayList<IType>();
      m_methodsTree.setContentProvider(new MethodsTreeContentProvider(types.toArray()));
      m_methodsTree.setInput(types.toArray());
      m_methodsTree.setSelection(new StructuredSelection(m_classUnderTest), true);
      doCheckedStateChanged();
      m_methodsTree.getControl().setFocus();     
    }
  }
  
  private void doCheckedStateChanged() {  
  }
  
  public IMethod[] getAllMethods() {
    return ((MethodsTreeContentProvider)m_methodsTree.getContentProvider()).getAllMethods();
  }

  public List<String> getSelectedMethods() {
    List<String> result = Lists.newArrayList();  
    Object[] checked= m_methodsTree.getCheckedElements();
    for (int i = 0; i < checked.length; i++) {
      if (checked[i] instanceof IMethod){
        IMethod method = (IMethod)checked[i];
        result.add(method.getElementName());
        
      }
    }   
    return result;
  }

  private static class MethodsTreeContentProvider implements ITreeContentProvider {
    private Object[] fTypes;
    private IMethod[] fMethods;
    private final Object[] fEmpty= new Object[0];

    public MethodsTreeContentProvider(Object[] types) {
      fTypes= types;
      Vector<IMethod> methods= new Vector<IMethod>();
      for (int i = types.length-1; i > -1; i--) {
        Object object = types[i];
        if (object instanceof IType) {
          IType type = (IType) object;
          try {
            IMethod[] currMethods= type.getMethods();
            for_currMethods:
            for (int j = 0; j < currMethods.length; j++) {
              IMethod currMethod = currMethods[j];
              int flags= currMethod.getFlags();
              if (!Flags.isPrivate(flags) && !Flags.isSynthetic(flags)) {
                for (int k = 0; k < methods.size(); k++) {
                  IMethod m= methods.get(k);
                  if (m.getElementName().equals(currMethod.getElementName())
                    && m.getSignature().equals(currMethod.getSignature())) {
                    methods.set(k,currMethod);
                    continue for_currMethods;
                  }
                }
                methods.add(currMethod);
              }
            }
          } catch (JavaModelException e) {
            TestNGPlugin.log(e);
          }
        }
      }
      fMethods= new IMethod[methods.size()];
      methods.copyInto(fMethods);
    }

    /*
     * @see ITreeContentProvider#getChildren(Object)
     */
    public Object[] getChildren(Object parentElement) {
      if (parentElement instanceof IType) {
        IType parentType= (IType)parentElement;
        ArrayList<IMethod> result= new ArrayList<IMethod>(fMethods.length);
        for (int i= 0; i < fMethods.length; i++) {
          if (fMethods[i].getDeclaringType().equals(parentType)) {
            result.add(fMethods[i]);
          }
        }
        return result.toArray();
      }
      return fEmpty;
    }

    /*
     * @see ITreeContentProvider#getParent(Object)
     */
    public Object getParent(Object element) {
      if (element instanceof IMethod)
        return ((IMethod)element).getDeclaringType();
      return null;
    }

    /*
     * @see ITreeContentProvider#hasChildren(Object)
     */
    public boolean hasChildren(Object element) {
      return getChildren(element).length > 0;
    }

    /*
     * @see IStructuredContentProvider#getElements(Object)
     */
    public Object[] getElements(Object inputElement) {
      return fTypes;
    }

    /*
     * @see IContentProvider#dispose()
     */
    public void dispose() {
    }

    /*
     * @see IContentProvider#inputChanged(Viewer, Object, Object)
     */
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    }

    public IMethod[] getAllMethods() {
      return fMethods;
    }
  }
}
