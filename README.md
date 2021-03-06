Disruptor Component
---
The **disruptor:** component provides asynchronous <a href="http://www.eecs.harvard.edu/~mdw/proj/seda/">SEDA</a> behavior much as the standard SEDA Component, but utilizes a
<a href="https://github.com/LMAX-Exchange/disruptor">Disruptor</a> instead of a
<a href="http://docs.oracle.com/javase/1.5.0/docs/api/java/util/concurrent/BlockingQueue.html">BlockingQueue</a> utilized by the
standard <a href="http://camel.apache.org/seda.html">SEDA Component</a>.

As with the SEDA component, queues are only visible within a *single*
<a href="http://camel.apache.org/camelcontext.html">CamelContext</a> and no support is provided for persistence or recovery.

The main advantage of choosing to use the Disruptor Component over the SEDA Component is performance in use cases where
there is high contention between producer(s) and/or multicasted or concurrent Consumers. In those cases, significant
increases of throughput and reduction of latency has been observed. Performance in scenarios without contention is
comparable to the SEDA Component.

The Disruptor is implemented with the intention of mimicing the behaviour and options of the SEDA Component as much as
possible. The main differences with the SEDA Component are the following:
* The buffer used is always bounded in size (default 1024 exchanges).
* As a the buffer is always bouded, the default behaviour for the Disruptor is to block while the buffer is full
instead of throwing an exception. This default behaviour may be configured on the component (see options).
* The Disruptor enpoints don't implement the BrowsableEndpoint interface. As such, the exchanges currently in the
Disruptor can't be retrieved, only the amount of exchanges.
* The Disruptor requires its consumers (multicasted or otherwise) to be statically configured. Adding or removing
consumers on the fly requires complete flushing of all pending exchanges in the Disruptor.
* As a result of the reconfiguration: Data sent over a Disruptor is directly processed and 'gone' if there is at least
one consumer, late joiners only get new exchanges published after they've joined.
* The *pollTimeout* option is not supported by the Disruptor Component.
* When a producer blocks on a full Disruptor, it does not respond to thread interrupts.

URI format
---
    disruptor:someName[?options]
Where **someName** can be any string that uniquely identifies the endpoint within the current
<a href="http://camel.apache.org/camelcontext.html">CamelContext</a>.
You can append query options to the URI in the following format:

    ?option=value&option=value&…

Options
---
<table>
<tr><td><strong>Name</strong></td><td><strong>Default</strong></td><td><strong>Description</strong></td></tr>
<tr>
<td>size</td>
<td>1024</td>
<td>The maximum capacity of the Disruptors ringbuffer. Will be effectively increased to the nearest power of two.
<strong>Notice:</strong> Mind if you use this option, then its the first endpoint being created with the queue name, that determines
the size. To make sure all endpoints use same size, then configure the size option on all of them, or the first endpoint
being created. </td>
</tr>

<tr>
<td>bufferSize</td>
<td></td>
<td><strong>Component only:</strong> The maximum default size (capacity of the number of messages it can hold) of the Disruptors
ringbuffer. This option is used if size is not in use.</td>
</tr>

<tr>
<td>queueSize</td>
<td></td>
<td><strong>Component only:</strong> Additional option to specify the <em>bufferSize</em> to maintain maximum compatibility with the SEDA
Component.</td>
</tr>

<tr>
<td>concurrentConsumers</td>
<td>1</td>
<td>Number of concurrent threads processing exchanges.</td>
</tr>

<tr>
<td>waitForTaskToComplete</td>
<td>IfReplyExpected</td>
<td>Option to specify whether the caller should wait for the async task to complete or not before continuing.
The following three options are supported: <em>Always</em>, <em>Never</em> or <em>IfReplyExpected</em>. The first two values are self-explanatory.
The last value, <em>IfReplyExpected</em>, will only wait if the message is
<a href="http://camel.apache.org/request-reply.html">Request Reply</a> based. See more information about
<a href="http://camel.apache.org/async.html">Async</a> messaging.</td>
</tr>

<tr>
<td>timeout</td>
<td>30000</td>
<td>Timeout (in milliseconds) before a producer will stop waiting for an asynchronous task to complete. See
<em>waitForTaskToComplete</em> and <a href="http://camel.apache.org/async.html">Async</a> for more details. You can disable timeout by
using 0 or a negative value.</td>
</tr>

<tr>
<td>multipleConsumers</td>
<td>false</td>
<td>Specifies whether multiple consumers are allowed. If enabled, you can use Disruptor for
<a href="http://en.wikipedia.org/wiki/Publish–subscribe_pattern">Publish-Subscribe</a> messaging. That is, you can send a message
to the Disruptor and have each consumer receive a copy of the message. When enabled, this option should be specified on
every consumer endpoint.</td>
</tr>

<tr>
<td>defaultMultipleConsumers</td>
<td></td>
<td><strong>Component only:</strong> Allows to set the default allowance of multiple consumers for endpoints created by this comonent
used when <em>multipleConsumers</em> is not provided.</td>
</tr>

<tr>
<td>limitConcurrentConsumers</td>
<td>true</td>
<td>Whether to limit the number of concurrentConsumers to the maximum of 500. By default, an exception will be thrown if
a Disruptor endpoint is configured with a greater number. You can disable that check by turning this option off.</td>
</tr>

<tr>
<td>blockWhenFull</td>
<td>true</td>
<td>Whether a thread that sends messages to a full Disruptor will block until the ringbuffer's capacity is no longer
exhausted. By default, the calling thread will block and wait until the message can be accepted. By disabling this
option, an exception will be thrown stating that the queue is full.</td>
</tr>

<tr>
<td>defaultBlockWhenFull</td>
<td></td>
<td><strong>Component only:</strong> Allows to set the default producer behaviour when the ringbuffer is full for endpoints created
by this comonent used when <em>blockWhenFull</em> is not provided.</td>
</tr>

<tr>
<td>waitStrategy</td>
<td>Blocking</td>
<td>Defines the strategy used by consumer threads to wait on new exchanges to be published. The options allowed are:
<em>Blocking</em>, <em>Sleeping</em>, <em>BusySpin</em> and <em>Yielding</em>. Refer to the section below for more information on this subject</td>
</tr>

<tr>
<td>defaultWaitStrategy</td>
<td></td>
<td><strong>Component only:</strong> Allows to set the default wait strategy for endpoints created by this comonent used when
<em>waitStrategy</em> is not provided.</td>
</tr>

<tr>
<td>producerType</td>
<td>Multi</td>
<td>Defines the producers allowed on the Disruptor. The options allowed are: <em>Multi</em> to allow multiple producers and
<em>Single</em> to enable certain optimizations only allowed when one concurrent producer (on one thread or otherwise
synchronized) is active.</td>
</tr>

<tr>
<td>defaultProducerType</td>
<td></td>
<td><strong>Component only:</strong> Allows to set the default producer type for endpoints created by this comonent used when
<em>producerType</em> is not provided.</td>
</tr>

</table>

Wait strategies
---
The wait strategy effects the type of waiting performed by the consumer threads that are currently waiting for the next
exchange to be published. The following strategies can be chosen:
<table>
<tr><td><strong>Name</strong></td><td><strong>Description</strong></td><td><strong>Advice</strong></td></tr>
<tr>
<td>Blocking</td>
<td>Blocking strategy that uses a lock and condition variable for Consumers waiting on a barrier.</td>
<td>This strategy can be used when throughput and low-latency are not as important as CPU resource. </td>
</tr>

<tr>
<td>Sleeping</td>
<td>Sleeping strategy that initially spins, then uses a Thread.yield(), and eventually for the minimum number of nanos
the OS and JVM will allow while the Consumers are waiting on a barrier.</td>
<td>This strategy is a good compromise between performance and CPU resource. Latency spikes can occur after quiet 
periods.</td>
</tr>

<tr>
<td>BusySpin</td>
<td>Busy Spin strategy that uses a busy spin loop for Consumers waiting on a barrier.</td>
<td>This strategy will use CPU resource to avoid syscalls which can introduce latency jitter.  It is best used when
threads can be bound to specific CPU cores.</td>
</tr>

<tr>
<td>Yielding</td>
<td>Yielding strategy that uses a Thread.yield() for Consumers waiting on a barrier after an initially spinning.</td>
<td>This strategy is a good compromise between performance and CPU resource without incurring significant latency
spikes.</td>
</tr>
</table>

Use of Request Reply
---
The Disruptor component supports using <a href="http://camel.apache.org/request-reply.html">Request Reply</a>, where the caller
will wait for the <a href="http://camel.apache.org/async.html">Async</a> route to complete. For instance:
    
    from("mina:tcp://0.0.0.0:9876?textline=true&sync=true").to("disruptor:input");
    from("disruptor:input").to("bean:processInput").to("bean:createResponse");
In the route above, we have a TCP listener on port 9876 that accepts incoming requests. The request is routed to the 
*disruptor:input* buffer. As it is a <a href="http://camel.apache.org/request-reply.html">Request Reply</a> message, we wait for
the response. When the consumer on the *disruptor:input* buffer is complete, it copies the response to the original
message response.

Concurrent consumers
---
By default, the Disruptor endpoint uses a single consumer thread, but you can configure it to use concurrent consumer
threads. So instead of thread pools you can use:

    from("disruptor:stageName?concurrentConsumers=5").process(...)
As for the difference between the two, note a thread pool can increase/shrink dynamically at runtime depending on load,
whereas the number of concurrent consumers is always fixed and supported by the Disruptor internally so performance will
be higher.

Thread pools
---
Be aware that adding a thread pool to a Disruptor endpoint by doing something like:

    from("disruptor:stageName").thread(5).process(...)
Can wind up with adding a normal
<a href="http://docs.oracle.com/javase/1.5.0/docs/api/java/util/concurrent/BlockingQueue.html">BlockingQueue</a> to be used in
conjunction with the Disruptor, effectively negating part of the performance gains achieved by using the Disruptor.
Instead, it is advices to directly configure number of threads that process messages on a Disruptor endpoint using the
*concurrentConsumers* option.

Sample
---
In the route below we use the Disruptor to send the request to this async queue to be able to send a fire-and-forget
message for further processing in another thread, and return a constant reply in this thread to the original caller.

    public void configure() throws Exception {
        from("direct:start")
            // send it to the disruptor that is async
            .to("disruptor:next")
            // return a constant response
            .transform(constant("OK"));

        from("disruptor:next").to("mock:result");
    }
Here we send a Hello World message and expects the reply to be OK.

    Object out = template.requestBody("direct:start", "Hello World");
    assertEquals("OK", out);
The "Hello World" message will be consumed from the Disruptor from another thread for further processing. Since this is
from a unit test, it will be sent to a mock endpoint where we can do assertions in the unit test.

Using multipleConsumers
---
In this example we have defined two consumers and registered them as spring beans.

    <!-- define the consumers as spring beans -->
    <bean id="consumer1" class="org.apache.camel.spring.example.FooEventConsumer"/>
    
    <bean id="consumer2" class="org.apache.camel.spring.example.AnotherFooEventConsumer"/>
    
    <camelContext xmlns="http://camel.apache.org/schema/spring">
        <!-- define a shared endpoint which the consumers can refer to instead of using url -->
        <endpoint id="foo" uri="disruptor:foo?multipleConsumers=true"/>
    </camelContext>
    
Since we have specified *multipleConsumers=true* on the Disruptor foo endpoint we can have those two or more consumers
receive their own copy of the message as a kind of pub-sub style messaging. As the beans are part of an unit test they
simply send the message to a mock endpoint, but notice how we can use @Consume to consume from the Disruptor.

    public class FooEventConsumer {

        @EndpointInject(uri = "mock:result")
        private ProducerTemplate destination;

        @Consume(ref = "foo")
        public void doSomething(String body) {
            destination.sendBody("foo" + body);
        }

    }

Extracting disruptor information
---
If needed, information such as buffer size, etc. can be obtained without using JMX in this fashion:

    DisruptorEndpoint disruptor = context.getEndpoint("disruptor:xxxx");
    int size = disruptor.getBufferSize();
