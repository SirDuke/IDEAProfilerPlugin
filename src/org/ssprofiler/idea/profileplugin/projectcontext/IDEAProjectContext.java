/*
 * Copyright 2012, Ivan Serduk. All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without modification, are
 *  permitted provided that the following conditions are met:
 *
 *     1. Redistributions of source code must retain the above copyright notice, this list of
 *        conditions and the following disclaimer.
 *
 *     2. Redistributions in binary form must reproduce the above copyright notice, this list
 *        of conditions and the following disclaimer in the documentation and/or other materials
 *        provided with the distribution.
 *
 *  THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESS OR IMPLIED
 *  WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 *  FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL Ivan Serduk OR
 *  CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 *  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 *  SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 *  ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 *  NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 *  ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.ssprofiler.idea.profileplugin.projectcontext;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.search.GlobalSearchScope;

import javax.swing.*;
import java.awt.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * User: Ivan Serduk
 * Date: 20.02.12
 */
public class IDEAProjectContext implements ProjectContext{
    private static final String ANONYMOUS_CLASS_REGEXP = "(.*)(\\$\\d+)(.*)";
    private static final Pattern ANONYMOUS_CLASS_PATTERN = Pattern.compile(ANONYMOUS_CLASS_REGEXP);
    
    private static final String CLASS_CONSTRUCTOR = "<init>";
    
    private Project project;
    
    public IDEAProjectContext(Project project) {
        this.project = project;
    }
    
    public void openSourceFile(String qualifiedMethodName) {
        GlobalSearchScope globalSearchScope = GlobalSearchScope.allScope(project);
        JavaPsiFacade javaPsiFacade = JavaPsiFacade.getInstance(project);
        
        int lastDot = qualifiedMethodName.lastIndexOf('.');
        
        String methodName = qualifiedMethodName.substring(lastDot + 1);
        
        String qualifiedClassName = qualifiedMethodName.substring(0, lastDot);

        Matcher anonymousClassMatcher = ANONYMOUS_CLASS_PATTERN.matcher(qualifiedClassName);
        if (anonymousClassMatcher.find()) {
            //Extensions.getExtensions(AnonymousElementProvider.EP_NAME)
            String nonAnonymousContainingClass = anonymousClassMatcher.group(1);
            nonAnonymousContainingClass = nonAnonymousContainingClass.replace('$', '.');
            PsiClass psiClass = javaPsiFacade.findClass(nonAnonymousContainingClass, globalSearchScope);
            if (psiClass != null) {
                psiClass.navigate(true);
                showMessage("Could not resolve anonymous classes. Showing outer non-anonymous class");
            } else {
                showMessage("Could not find class with name: ");
                showMessage(nonAnonymousContainingClass);
            }
        } else {
            qualifiedClassName = qualifiedClassName.replace('$', '.');
            PsiClass psiClass = javaPsiFacade.findClass(qualifiedClassName, globalSearchScope);
            if (psiClass != null) {
                PsiMethod[] methods;
                if (CLASS_CONSTRUCTOR.equals(methodName)) {
                    methods = psiClass.getConstructors();
                } else {
                    methods = psiClass.findMethodsByName(methodName, false);
                }
                if (methods.length == 1) {
                    methods[0].navigate(true);
                } else if (methods.length == 0) {
                    psiClass.navigate(true);
                    showMessage("Could not find method with name \"" + methodName + "\"");
                } else {
                    psiClass.navigate(true);
                    showMessage("There are several methods with name \"" + methodName +  "\"");
                }
            } else {
                showMessage("Could not find class with name:");
                showMessage(qualifiedClassName);
            }
        }
    }
    
    private void showMessage(String msg) {
        WindowManager.getInstance().getStatusBar(project).fireNotificationPopup(new JLabel(msg), Color.RED);
    }
}
