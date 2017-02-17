package com.stony.mango.plugin.provider;

import com.google.common.base.Optional;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider;
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.NotificationListener.Adapter;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.PsiClassReferenceType;
import com.intellij.psi.search.searches.MethodReferencesSearch;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.util.Processor;
import com.intellij.util.Query;
import com.oracle.tools.packager.Log;
import com.stony.mango.plugin.annotation.Annotation;
import com.stony.mango.plugin.util.JavaUtils;
import icons.SpringApiIcons;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * <p>mango-plugin
 * <p>com.stony.mango.plugin
 *
 * @author stony
 * @version 下午4:59
 * @since 2017/2/16
 */
public class DaoLineMarkerProvider extends RelatedItemLineMarkerProvider {


    @Override
    protected void collectNavigationMarkers(@NotNull PsiElement element, Collection<? super RelatedItemLineMarkerInfo> result) {
        if(element instanceof PsiClass){
            PsiClass psiClass = (PsiClass) element;
            if (!psiClass.isInterface()) return;
            if (!isTargetClass(psiClass))  return;
            Set<PsiElement> elements = new HashSet<>();
            Query<PsiReference> psiReferences =  ReferencesSearch.search(element);
            psiReferences.forEach(new Processor<PsiReference>() {
                @Override
                public boolean process(PsiReference psiReference) {
                    PsiElement _psiElement = psiReference.getElement();
                    if(_psiElement.getParent() == null || !isImport(_psiElement.getParent())) {
                        elements.add(_psiElement);
                    }
                    return true;
                }
            });
//            message(element.getProject(),psiClass.getName() + " = " + elements);

            NavigationGutterIconBuilder<PsiElement> builder  =
                    NavigationGutterIconBuilder.create(SpringApiIcons.SpringJavaBean)
                            .setAlignment(GutterIconRenderer.Alignment.CENTER)
//                            .setTarget(psiClass)
                            .setTargets(elements)
                            .setTooltipTitle("Dao - " + psiClass.getQualifiedName());
            result.add(builder.createLineMarkerInfo(psiClass.getNameIdentifier()));
        } else if(element instanceof PsiMethod){
            PsiMethod psiMethod = (PsiMethod) element;
            if (!isTargetMethod(psiMethod))  return;


            Set<PsiElement> elements = new HashSet<>();
            Query<PsiReference> psiReferences =  MethodReferencesSearch.search(psiMethod);
            psiReferences.forEach(new Processor<PsiReference>() {
                @Override
                public boolean process(PsiReference psiReference) {
                    elements.add(psiReference.getElement());
                    return true;
                }
            });
            PsiClass psiClass = psiMethod.getContainingClass();
            NavigationGutterIconBuilder<PsiElement> builder  =
                    NavigationGutterIconBuilder.create(SpringApiIcons.SpringBeanMethod)
                            .setAlignment(GutterIconRenderer.Alignment.CENTER)
//                            .setTarget(psiClass)
                            .setTargets(elements)
                            .setTooltipTitle("Dao - " + psiClass.getQualifiedName());
            result.add(builder.createLineMarkerInfo(psiMethod.getNameIdentifier()));
        } else if(element instanceof PsiField) {

            PsiField field = (PsiField) element;
            if (!isTargetField(field))  return;
            PsiType type = field.getType();
            if (!(type instanceof PsiClassReferenceType)) return;

            Optional<PsiClass> clazz = JavaUtils.findClazz(element.getProject(), type.getCanonicalText());
            if (!clazz.isPresent()) return;

            PsiClass psiClass = clazz.get();
            if (!isTargetClass(psiClass)) return;

            NavigationGutterIconBuilder<PsiElement> builder  =
                    NavigationGutterIconBuilder.create(SpringApiIcons.ShowAutowiredDependencies)
                            .setAlignment(GutterIconRenderer.Alignment.CENTER)
                            .setTarget(psiClass)
                            .setTooltipTitle("Data access object found - " + psiClass.getQualifiedName());
            result.add(builder.createLineMarkerInfo(field.getNameIdentifier()));
        }

    }

   boolean isImport(PsiElement element){
       return element instanceof PsiImportStatement;
   }
   void message(final Project project, String content){
        Notification notification = new Notification("Mango-Plugin", "Mango Plugin", content,  NotificationType.INFORMATION);
        notification.notify(project);
    }


    private boolean isTargetClass(PsiClass clazz) {
        if(clazz == null) return false;
        if (JavaUtils.isAnnotationPresent(clazz, Annotation.DB)) {
            return true;
        }
        return false;
    }
    private boolean isTargetMethod(PsiMethod method) {
        if (JavaUtils.isAnnotationPresent(method, Annotation.SQL)) {
            return true;
        }
        return false;
    }
    private boolean isTargetField(PsiField field) {
        if (JavaUtils.isAnnotationPresent(field, Annotation.AUTOWIRED)) {
            return true;
        }
        if (JavaUtils.isAnnotationPresent(field, Annotation.RESOURCE)) {
            return true;
        }
        Optional<PsiAnnotation> resourceAnno = JavaUtils.getPsiAnnotation(field, Annotation.RESOURCE);
        if (resourceAnno.isPresent()) {
            PsiAnnotationMemberValue nameValue = resourceAnno.get().findAttributeValue("name");
            String name = nameValue.getText().replaceAll("\"", "");
            return StringUtils.isBlank(name) || name.equals(field.getName());
        }
        return false;
    }

}
