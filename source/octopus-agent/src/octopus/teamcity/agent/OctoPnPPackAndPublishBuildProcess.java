/*
 * Copyright 2000-2012 Octopus Deploy Pty. Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package octopus.teamcity.agent;

import jetbrains.buildServer.ExtensionHolder;
import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.AgentRunningBuild;
import jetbrains.buildServer.agent.ArtifactsPreprocessor;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.agent.BuildRunnerContext;
import jetbrains.buildServer.agent.impl.artifacts.ArtifactsBuilder;
import jetbrains.buildServer.agent.impl.artifacts.ArtifactsCollection;
import jetbrains.buildServer.messages.serviceMessages.ServiceMessage;
import jetbrains.buildServer.util.StringUtil;
import octopus.teamcity.common.OctopusConstants;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class OctoPnPPackAndPublishBuildProcess extends OctopusBuildProcess {

    protected final ExtensionHolder myExtensionHolder;
    protected final AgentRunningBuild myRunningBuild;
    protected List<ArtifactsCollection> artifactsCollections;

    public OctoPnPPackAndPublishBuildProcess(@NotNull AgentRunningBuild runningBuild, @NotNull BuildRunnerContext context, @NotNull final ExtensionHolder extensionHolder) {
       super(runningBuild, context);

        myExtensionHolder = extensionHolder;
        myRunningBuild = runningBuild;
    }

    @Override
    protected String getLogMessage() {
        return "Packing and publishing deployment packages to Octopus server";
    }

    @Override
    public void start() throws RunBuildException {
        final Collection<ArtifactsPreprocessor> preprocessors = myExtensionHolder.getExtensions(ArtifactsPreprocessor.class);

        final Map<String, String> parameters = getContext().getRunnerParameters();
        final OctopusConstants constants = OctopusConstants.Instance;
        final String nuspecPaths = parameters.get(constants.getNuspecPathsKey());

        runningBuild.getBuildLogger().message("nuspecPaths: " + nuspecPaths);

        final ArtifactsBuilder builder = new ArtifactsBuilder();
        builder.setPreprocessors(preprocessors);
        builder.setBaseDir(myRunningBuild.getCheckoutDirectory());
        builder.setArtifactsPaths(nuspecPaths);
        artifactsCollections = builder.build();

        extractNugetExe();
        OctopusCommandBuilder arguments = createCommand();
        runNuget(arguments);

        /*BuildProgressLogger logger = myRunningBuild.getBuildLogger();
        for (ArtifactsCollection artifactsCollection : artifactsCollections) {
            for (Map.Entry<File, String> fileStringEntry : artifactsCollection.getFilePathMap().entrySet()) {
                final File source = fileStringEntry.getKey();

                String message = ServiceMessage.asString("publishArtifacts", source.getAbsolutePath());
                logger.message(message);
            }
        }*/
    }

    @Override
    protected OctopusCommandBuilder createCommand() {
        final Map<String, String> parameters = getContext().getRunnerParameters();
        final OctopusConstants constants = OctopusConstants.Instance;

        return new OctopusCommandBuilder() {
            @Override
            protected String[] buildCommand(boolean masked) {
                final ArrayList<String> commands = new ArrayList<String>();
                final String serverUrl = parameters.get(constants.getServerKey());
                final String apiKey = parameters.get(constants.getApiKey());

                String packageVersion = null;
                final String packageVersionKey = constants.getPackageVersionKey();
                if (parameters.containsKey(packageVersionKey)) {
                    packageVersion = parameters.get(packageVersionKey);
                }

                commands.add("pack");
                for (ArtifactsCollection artifactsCollection : artifactsCollections) {
                    for (Map.Entry<File, String> fileStringEntry : artifactsCollection.getFilePathMap().entrySet()) {
                        final File source = fileStringEntry.getKey();
                        commands.add(source.getAbsolutePath());
                    }
                }
                commands.add("-NoPackageAnalysis");
                if (!StringUtil.isEmptyOrSpaces(packageVersion))
                {
                    commands.add("-Version");
                    commands.add(packageVersion);
                }
                commands.add("-OutputDirectory");
                commands.add(extractedTo.getAbsolutePath());

                /*commands.add("--server");
                commands.add(serverUrl);
                commands.add("--apikey");
                commands.add(masked ? "SECRET" : apiKey);*/

                return commands.toArray(new String[commands.size()]);
            }
        };
    }
}
