jocean-event-extend
==============

jocean : event's Extend Impl for J2SE

2015-11-21: release 0.0.4 版本:
    
    1、fix bug: NPE when EventEngine.create 3rd parameter is null
    2、精简EventEngine接口:去掉createFromInnerState接口;改变create方法签名，添加参数name,用来指定EventReceiver名字(调试用途),而原来的flow不再是必选参数，可提供0个或多个reactors，reactor可实现
        EventNameAware, 
        EventHandlerAware,
        EndReasonProvider,
        EndReasonAware,
        ExectionLoopAware,
        FlowLifecycleListener,
        FlowStateChangedListener接口
    3、interface rename: EventReceiverSource --> EventEngine
    4、支持新增flow业务流程类实现FlowStateChangedListener接口，在状态改变(包括流程结束时)触发对应的onStateChanged方法
    5、upgrade com.google.guava:guava from 16.0.1 -> 18.0
    6、使用 gradle 构建
    7、对 FlowRunner 中生成的 EventReceiver 实例，重载其 toString方法，显示对应的flow 实例 toString; 加强调试日志的可读性
        
2014-08-19： release 0.0.3 版本：
    
    1、在FlowRunner中添加getFlowsDetail方法，获取当前所有Flow的细节信息，包括 业务类/状态/创建时间/最后切换状态时间/tta/ttl/结束原因，依赖 jocean-event-core-0.1.3-release
