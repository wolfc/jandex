/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.jandex.test;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.jandex.Indexer;
import org.jboss.jandex.MethodInfo;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class MethodTestCase {
    @Retention(RetentionPolicy.RUNTIME)
    @interface Stateless {}

    @Retention(RetentionPolicy.RUNTIME)
    @interface EJB {}

    @Stateless
    static class MyBean {
        @EJB
        public void setSomeBean(Object other) {
        }
    }

    @Stateless
    static class SubBean extends MyBean {
    }

    private static AnnotationInstance find(Collection<AnnotationInstance> annotationInstances, Class<?> key) {
        DotName nameKey = DotName.createSimple(key.getName());
        for(AnnotationInstance annotationInstance : annotationInstances) {
            AnnotationTarget target = annotationInstance.target();
            if(target instanceof ClassInfo) {
                ClassInfo classInfo = (ClassInfo) target;
                if(classInfo.name().equals(nameKey))
                    return annotationInstance;
            }
        }
        throw new IllegalStateException("Can't find " + key);
    }

    private static void index(Indexer indexer, Class<?> cls) throws IOException {
        InputStream stream = cls.getClassLoader().getResourceAsStream(cls.getName().replace('.', '/') + ".class");
        try {
            indexer.index(stream);
        }
        finally {
            stream.close();
        }
    }

    @Test
    public void testMethod() throws Exception {
        Indexer indexer = new Indexer();
        index(indexer, MyBean.class);
        Index index = indexer.complete();

        List<AnnotationInstance> annotations = index.getAnnotations(DotName.createSimple(Stateless.class.getName()));
        AnnotationInstance annotationInstance = annotations.get(0);
        ClassInfo classInfo = (ClassInfo) annotationInstance.target();
        MethodInfo methodInfo = (MethodInfo) classInfo.annotations().get(DotName.createSimple(EJB.class.getName())).get(0).target();
        assertEquals(classInfo, methodInfo.declaringClass());
        assertEquals("setSomeBean", methodInfo.name());
    }

    @Test
    public void testSubMethod() throws Exception {
        Indexer indexer = new Indexer();
        index(indexer, SubBean.class);
        index(indexer, MyBean.class);
        Index index = indexer.complete();

        List<AnnotationInstance> annotations = index.getAnnotations(DotName.createSimple(Stateless.class.getName()));
        AnnotationInstance annotationInstance = find(annotations, SubBean.class);
        ClassInfo classInfo = (ClassInfo) annotationInstance.target();
        classInfo = index.getClassByName(classInfo.superName());
        List<AnnotationInstance> ejbAnnotations = classInfo.annotations().get(DotName.createSimple(EJB.class.getName()));
        assertNotNull(ejbAnnotations);
        MethodInfo methodInfo = (MethodInfo) ejbAnnotations.get(0).target();
        assertEquals(classInfo, methodInfo.declaringClass());
        assertEquals("setSomeBean", methodInfo.name());
    }
}
