import gradlebuild.basics.BuildEnvironmentExtension
import gradlebuild.basics.git
import gradlebuild.basics.parentOrRoot

/*
 * Copyright 2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


val buildEnvironmentExtension = extensions.create("buildEnvironment", BuildEnvironmentExtension::class)
// TODO: Anze: Check why IntelliJ shows an error
buildEnvironmentExtension.gitCommitId = git("rev-parse", "HEAD")
buildEnvironmentExtension.gitBranch = git("rev-parse", "--abbrev-ref", "HEAD")
buildEnvironmentExtension.repoRoot = layout.projectDirectory.parentOrRoot()
