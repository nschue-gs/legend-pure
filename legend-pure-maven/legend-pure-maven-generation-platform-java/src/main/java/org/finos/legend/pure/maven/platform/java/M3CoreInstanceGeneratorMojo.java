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

package org.finos.legend.pure.maven.platform.java;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.pure.m3.generator.bootstrap.M3CoreInstanceGenerator;
import org.finos.legend.pure.m3.generator.par.Log;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.finos.legend.pure.runtime.java.compiled.generation.orchestrator.JavaCodeGeneration.durationSinceInSeconds;


@Mojo(name = "generate-m3-core-instances")
public class M3CoreInstanceGeneratorMojo extends AbstractMojo
{
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    protected MavenProject project;

    @Parameter(defaultValue = "false")
    private boolean skip;

    @Parameter(property = "outputDir", required = true)
    private String outputDir;

    @Parameter(property = "factoryNamePrefix", required = true)
    private String factoryNamePrefix;

    @Parameter(property = "fileNameSet", required = true)
    private Set<String> fileNameSet;

    @Parameter(property = "fileNameStartsWith")
    private String fileNameStartsWith;

    @Parameter(property = "fileLocation")
    private String fileLocation;


    @Override
    public void execute() throws MojoExecutionException
    {
        Log log = new Log()
        {
            @Override
            public void info(String txt)
            {
                getLog().info(txt);
            }

            @Override
            public void error(String txt, Exception e)
            {
                getLog().error(txt, e);
            }
        };

        ClassLoader savedClassLoader = Thread.currentThread().getContextClassLoader();
        long start = System.nanoTime();
        try
        {
            Thread.currentThread().setContextClassLoader(buildClassLoader(this.project, savedClassLoader, log));
            M3CoreInstanceGenerator.generate(outputDir, factoryNamePrefix, Sets.mutable.withAll(fileNameSet), fileNameStartsWith);
        }
        catch (Exception e)
        {
            log.error(String.format("    Failed to generate M3 core instances (%.9fs)", durationSinceInSeconds(start)), e);
            throw new MojoExecutionException("Failed to generate M3 core instances", e);
        }
        finally
        {
            Thread.currentThread().setContextClassLoader(savedClassLoader);
        }
    }

    private ClassLoader buildClassLoader(MavenProject project, ClassLoader parent, Log log) throws DependencyResolutionRequiredException
    {
        // Add the project output to the plugin classloader
        List<String> classpathElements = project.getCompileClasspathElements();

        classpathElements.add(this.fileLocation);
        classpathElements.add(this.project.getBuild().getOutputDirectory());

        URL[] urlsForClassLoader = ListIterate.collect(classpathElements, mavenCompilePath ->
        {
            try
            {
                return Paths.get(mavenCompilePath).toUri().toURL();
            }
            catch (MalformedURLException e)
            {
                throw new RuntimeException(e);
            }
        }).toArray(new URL[0]);
        log.info("    Project classLoader URLs " + Arrays.toString(urlsForClassLoader));
        return new URLClassLoader(urlsForClassLoader, parent);
    }
}