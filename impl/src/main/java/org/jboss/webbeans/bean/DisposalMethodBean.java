/*
 * JBoss, Home of Professional Open Source
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,  
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.webbeans.bean;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Initializer;
import javax.enterprise.inject.Produces;

import org.jboss.webbeans.BeanManagerImpl;
import org.jboss.webbeans.DefinitionException;
import org.jboss.webbeans.bootstrap.BeanDeployerEnvironment;
import org.jboss.webbeans.injection.MethodInjectionPoint;
import org.jboss.webbeans.injection.WBInjectionPoint;
import org.jboss.webbeans.introspector.WBMethod;

public class DisposalMethodBean<T> extends AbstractReceiverBean<T, Method>
{

   protected MethodInjectionPoint<T> disposalMethodInjectionPoint;
   private final String id;

   protected DisposalMethodBean(BeanManagerImpl manager, WBMethod<T> disposalMethod, AbstractClassBean<?> declaringBean)
   {
      super(declaringBean, manager);
      this.disposalMethodInjectionPoint = MethodInjectionPoint.of(this, disposalMethod);
      this.id = createId("DisposalMethod-" + declaringBean.getName() + "-" + disposalMethod.getSignature().toString());
      initBindings();
      initType();
      initTypes();
   }
   
   @Override
   public void initialize(BeanDeployerEnvironment environment)
   {
      // TODO Auto-generated method stub
      super.initialize(environment);
      checkDisposalMethod();
   }

   @SuppressWarnings("unchecked")
   protected void initType()
   {
      this.type = (Class<T>) disposalMethodInjectionPoint.getAnnotatedParameters(Disposes.class).get(0).getJavaClass();
   }

   @Override
   public WBMethod<T> getAnnotatedItem()
   {
      return disposalMethodInjectionPoint;
   }

   public static <T> DisposalMethodBean<T> of(BeanManagerImpl manager, WBMethod<T> disposalMethod, AbstractClassBean<?> declaringBean)
   {
      return new DisposalMethodBean<T>(manager, disposalMethod, declaringBean);
   }

   protected void initInjectionPoints()
   {
      injectionPoints.add(disposalMethodInjectionPoint);
   }

   @Override
   protected void initBindings()
   {
      // At least 1 parameter exists, already checked in constructor
      this.bindings = new HashSet<Annotation>();
      this.bindings.addAll(disposalMethodInjectionPoint.getParameters().get(0).getBindings());
      initDefaultBindings();
   }

   /**
    * Initializes the API types
    */
   @Override
   protected void initTypes()
   {
      Set<Type> types = new HashSet<Type>();
      types = new HashSet<Type>();
      types.addAll(disposalMethodInjectionPoint.getAnnotatedParameters(Disposes.class).get(0).getTypeClosure());
      types.add(Object.class);
      super.types = types;
   }

   @Override
   public Set<WBInjectionPoint<?, ?>> getAnnotatedInjectionPoints()
   {
      return injectionPoints;
   }

   @Override
   public String getName()
   {
      return null;
   }

   @Override
   public Class<? extends Annotation> getScopeType()
   {
      return null;
   }

   @Override
   public Set<Type> getTypes()
   {
      return types;
   }

   @Override
   public String toString()
   {
      return disposalMethodInjectionPoint.toString();
   }

   @Override
   public boolean isNullable()
   {
      // Not relevant
      return false;
   }

   @Override
   public boolean isSerializable()
   {
      // Not relevant
      return false;
   }

   @Override
   public boolean isProxyable()
   {
      return true;
   }

   public T create(CreationalContext<T> creationalContext)
   {
      // Not Relevant
      return null;
   }

   public void invokeDisposeMethod(Object instance, CreationalContext<?> creationalContext)
   {
      Object receiverInstance = getReceiver(creationalContext);
      if (receiverInstance == null)
      {
         disposalMethodInjectionPoint.invokeWithSpecialValue(null, Disposes.class, instance, manager, creationalContext, IllegalArgumentException.class);
      }
      else
      {
         disposalMethodInjectionPoint.invokeOnInstanceWithSpecialValue(receiverInstance, Disposes.class, instance, manager, creationalContext, IllegalArgumentException.class);
      }
   }

   private void checkDisposalMethod()
   {
      if (!disposalMethodInjectionPoint.getParameters().get(0).isAnnotationPresent(Disposes.class))
      {
         throw new DefinitionException(disposalMethodInjectionPoint.toString() + " doesn't have @Dispose as first parameter");
      }
      if (disposalMethodInjectionPoint.getAnnotatedParameters(Disposes.class).size() > 1)
      {
         throw new DefinitionException(disposalMethodInjectionPoint.toString() + " has more than one @Dispose parameters");
      }
      if (disposalMethodInjectionPoint.getAnnotatedParameters(Observes.class).size() > 0)
      {
         throw new DefinitionException("@Observes is not allowed on disposal method, see " + disposalMethodInjectionPoint.toString());
      }
      if (disposalMethodInjectionPoint.getAnnotation(Initializer.class) != null)
      {
         throw new DefinitionException("@Intitializer is not allowed on a disposal method, see " + disposalMethodInjectionPoint.toString());
      }
      if (disposalMethodInjectionPoint.getAnnotation(Produces.class) != null)
      {
         throw new DefinitionException("@Produces is not allowed on a disposal method, see " + disposalMethodInjectionPoint.toString());
      }
      if (getDeclaringBean() instanceof EnterpriseBean<?>)
      {
         boolean methodDeclaredOnTypes = false;
         // TODO use annotated item?
         for (Type type : getDeclaringBean().getTypes())
         {
            if (type instanceof Class<?>)
            {
               Class<?> clazz = (Class<?>) type;
               try
               {
                  clazz.getDeclaredMethod(disposalMethodInjectionPoint.getName(), disposalMethodInjectionPoint.getParameterTypesAsArray());
                  methodDeclaredOnTypes = true;
               }
               catch (NoSuchMethodException nsme)
               {
                  // No - op
               }
            }
         }
         if (!methodDeclaredOnTypes)
         {
            throw new DefinitionException("Producer method " + toString() + " must be declared on a business interface of " + getDeclaringBean());
         }
      }
   }

   @Override
   public Class<T> getType()
   {
      return type;
   }

   @Override
   protected String getDefaultName()
   {
      return disposalMethodInjectionPoint.getPropertyName();
   }

   public void destroy(T instance, CreationalContext<T> creationalContext)
   {
      // No-op. Producer method dependent objects are destroyed in producer method bean  
   }

   @Override
   public String getId()
   {
      return id;
   }

   public boolean isPolicy()
   {
      return false;
   }

   @Override
   public AbstractBean<?, ?> getSpecializedBean()
   {
      // Doesn't support specialization
      return null;
   }
   
   @Override
   protected void initScopeType()
   {
      // Disposal methods aren't scoped
   }

   @Override
   public Class<? extends Annotation> getDeploymentType()
   {
      return getDeclaringBean().getDeploymentType();
   }

   @Override
   protected void initDeploymentType()
   {
      // Not used
   }
   
   @Override
   protected void checkDeploymentType()
   {
      // TODO sort out ordering of init, then we can use initDeploymentType and hence checkDeploymentType
   }

}
