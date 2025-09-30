// Copyright 2025 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.finos.legend.pure.runtime.java.interpreted.profiler;

import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.stack.MutableStack;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.runtime.java.interpreted.ExecutionSupport;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.finos.legend.pure.runtime.java.interpreted.VariableContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.InstantiationContext;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Stack;
import java.util.UUID;

public class FunctionExecutionInterpretedWithTiming extends FunctionExecutionInterpreted
{
    private final PrintWriter writer;

    public FunctionExecutionInterpretedWithTiming(Path coverageDirectory)
    {
        super();
        try
        {
            Files.createDirectories(coverageDirectory);
            this.writer = new PrintWriter(Files.newOutputStream(coverageDirectory.resolve(UUID.randomUUID() + ".puretime"), StandardOpenOption.CREATE_NEW));
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public CoreInstance start(CoreInstance function, ListIterable<? extends CoreInstance> arguments)
    {
        try
        {
            return super.start(function, arguments);
        }
        finally
        {
            this.writer.flush();
        }
    }

    @Override
    public CoreInstance executeFunction(boolean limitScope, Function<?> function, ListIterable<? extends CoreInstance> params, Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, VariableContext varContext, MutableStack<CoreInstance> functionExpressionCallStack, Profiler profiler, InstantiationContext instantiationContext, ExecutionSupport executionSupport)
    {
        UUID executionId = UUID.randomUUID();
        SourceInformation info = function.getSourceInformation();
        if (info != null)
        {
            this.writer.printf("Starting execution: %s %d %d %d %d %s%n", info.getSourceId(), info.getStartLine() - 1, info.getStartColumn() - 1, info.getEndLine() - 1, info.getEndColumn(), executionId);
        }
        long startTime = System.currentTimeMillis();
        try
        {
            return super.executeFunction(limitScope, function, params, resolvedTypeParameters, resolvedMultiplicityParameters, varContext, functionExpressionCallStack, profiler, instantiationContext, executionSupport);
        }
        finally
        {
            long elapsedTimeMillis = System.currentTimeMillis() - startTime;
            if (info != null)
            {
                this.writer.printf("Completed execution: %s %s%n", executionId, formatElapsed(elapsedTimeMillis));
            }
            this.writer.flush();
        }
    }

    private String formatElapsed(long elapsedTime)
    {
        return String.format("%d.%03d seconds", elapsedTime / 1000, elapsedTime % 1000);
    }
}
