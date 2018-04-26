package com.anaconda.skein;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.apache.hadoop.yarn.api.ApplicationConstants;
import org.apache.hadoop.yarn.api.records.LocalResource;
import org.apache.hadoop.yarn.api.records.LocalResourceType;
import org.apache.hadoop.yarn.api.records.Resource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Model {
  private static void throwIfNull(Object obj, String param)
      throws IllegalArgumentException {
    if (obj == null) {
      throw new IllegalArgumentException(param + " must be non-null");
    }
  }

  private static void throwIfNonPositive(int i, String param)
      throws IllegalArgumentException {
    if (i <= 0) {
      throw new IllegalArgumentException(param + " must be > 0");
    }
  }

  public static class File {
    private String source;
    private String dest;
    private LocalResourceType type;

    public File() {}

    public File(String source, String dest, LocalResourceType type) {
      this.source = source;
      this.dest = dest;
      this.type = type;
    }

    public String toString() {
      return ("File<"
              + "source=" + source + ", "
              + "dest=" + dest + ", "
              + "type=" + type + ">");
    }

    public void setSource(String source) { this.source = source; }
    public String getSource() { return source; }

    public void setDest(String dest) { this.dest = dest; }
    public String getDest() { return dest; }

    public void setType(LocalResourceType type) { this.type = type; }
    public LocalResourceType getType() { return type; }

    public void validate() throws IllegalArgumentException {
      throwIfNull(source, "source");
      throwIfNull(dest, "dest");
      throwIfNull(type, "type");
      if (type.equals(LocalResourceType.PATTERN)) {
        throw new IllegalArgumentException("PATTERN type not currently supported");
      }
    }
  }

  @JsonIgnoreProperties({"name",
                         "count",
                         "preparedCommands",
                         "preparedEnv"})
  public static class Service {
    // Serialized state
    private int instances;
    private Resource resources;
    private List<File> files;
    private Map<String, LocalResource> localResources;
    private Map<String, String> env;
    private List<String> commands;
    private Set<String> depends;

    // Runtime state
    private String name;
    private int count = 0;
    private List<String> preparedCommands;
    private Map<String, String> preparedEnv;

    public Service() {}

    public Service(int instances, Resource resources, List<File> files,
                   Map<String, String> env, List<String> commands,
                   Set<String> depends) {
      this.instances = instances;
      this.resources = resources;
      this.files = files;
      this.env = env;
      this.commands = commands;
      this.depends = depends;
    }

    public Service(int instances, Resource resources,
                   Map<String, LocalResource> localResources,
                   Map<String, String> env, List<String> commands,
                   Set<String> depends) {
      this.instances = instances;
      this.resources = resources;
      this.localResources = localResources;
      this.env = env;
      this.commands = commands;
      this.depends = depends;
    }

    public String toString() {
      return ("Service:\n"
              + "instances: " + instances + "\n"
              + "resources: " + resources + "\n"
              + "files: " + files + "\n"
              + "localResources: " + localResources + "\n"
              + "env: " + env + "\n"
              + "commands: " + commands + "\n"
              + "depends: " + depends);
    }

    public void setInstances(int instances) { this.instances = instances; }
    public int getInstances() { return instances; }

    public void setResources(Resource resources) { this.resources = resources; }
    public Resource getResources() { return resources; }

    public void setFiles(List<File> files) { this.files = files; }
    public List<File> getFiles() { return files; }

    public void setLocalResources(Map<String, LocalResource> r) { this.localResources = r; }
    public Map<String, LocalResource> getLocalResources() { return localResources; }

    public void setEnv(Map<String, String> env) { this.env = env; }
    public Map<String, String> getEnv() { return env; }

    public void setCommands(List<String> commands) { this.commands = commands; }
    public List<String> getCommands() { return commands; }

    public void setDepends(Set<String> depends) { this.depends = depends; }
    public Set<String> getDepends() { return depends; }

    private String formatConfig(Map<String, String> config, String val) {
      if (config != null) {
        for (Map.Entry<String, String> item : config.entrySet()) {
          val = val.replace("%(" + item.getKey() + ")", item.getValue());
        }
      }
      return val;
    }

    public void prepare(Map<String, String> config) {
      preparedCommands = new ArrayList<String>();
      String logdir = ApplicationConstants.LOG_DIR_EXPANSION_VAR;
      String pipeLogs = (" 1>>" + logdir + "/" + name + ".stdout "
                         + "2>>" + logdir + "/" + name + ".stderr;");
      for (String c : commands) {
        preparedCommands.add(formatConfig(config, c) + pipeLogs);
      }

      preparedEnv = new HashMap<String, String>();
      for (Map.Entry<String, String> item : env.entrySet()) {
        preparedEnv.put(item.getKey(), formatConfig(config, item.getValue()));
      }
    }

    public void setName(String name) {
      this.name = name;
    }

    public void validate(boolean uploaded) throws IllegalArgumentException {
      throwIfNonPositive(instances, "instances");

      throwIfNull(resources, "resources");
      throwIfNonPositive(resources.getMemory(), "resources.memory");
      throwIfNonPositive(resources.getVirtualCores(), "resources.vcores");

      // User -> Client: files is set, localResources is null
      // Client -> ApplicationMaster: localResources is set, files is null
      if (uploaded) {
        if (files != null) {
          throw new IllegalArgumentException("unexpected field: files");
        }
      } else {
        if (localResources != null) {
          throw new IllegalArgumentException("unexpected field: localResources");
        }
        if (files != null) {
          for (File f : files) {
            f.validate();
          }
        }
      }
    }
  }

  public static class Job {
    private String name;
    private String queue;
    private Map<String, Service> services;

    public Job() {}

    public Job(String name, String queue, Map<String, Service> services) {
      this.name = name;
      this.queue = queue;
      this.services = services;
    }

    public String toString() {
      return ("Job<"
              + "name: " + name + ", "
              + "queue: " + queue + ", "
              + "services: " + services + ">");
    }

    public void setName(String name) { this.name = name; }
    public String getName() { return name; }

    public void setQueue(String queue) { this.queue = queue; }
    public String getQueue() { return queue; }

    public void setServices(Map<String, Service> services) { this.services = services; }
    public Map<String, Service> getServices() { return services; }

    public void validate(boolean uploaded) throws IllegalArgumentException {
      throwIfNull(name, "name");
      throwIfNull(queue, "queue");
      throwIfNull(services, "services");
      if (services.size() == 0) {
        throw new IllegalArgumentException("There must be at least one service");
      }
      for (Service s: services.values()) {
        s.validate(uploaded);
      }
    }
  }

  public static enum ContainerState {
    WAITING,    // Waiting on service dependencies
    REQUESTED,  // Container requested, waiting to run
    RUNNING,    // Currently running
    FINISHED,   // Successfully finished running
    FAILED,     // Errored or was killed by yarn
    STOPPED     // Stopped by user
  }

  public static class Container {
    private Service service;
    private ContainerState state;

    public Container(Service service, ContainerState state) {
      this.service = service;
      this.state = state;
    }

    public ContainerState getState() { return state; }
    public void setState(ContainerState state) { this.state = state; }

    public String toString() {
      return "Container<state: " + state + ">";
    }
  }
}