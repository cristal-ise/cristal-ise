### Step 1: Serializer foundation (no runtime behavior change)

- Goal:
  - Introduce serializer abstraction in kernel without changing runtime behavior.
- Files to create:
  - `kernel/src/main/java/org/cristalise/kernel/utils/KernelSerializer.java`
  - `kernel/src/main/java/org/cristalise/kernel/utils/CastorSerializerAdapter.java`
  - `kernel/src/main/java/org/cristalise/kernel/utils/VertxJsonSerializer.java`
  - `kernel/src/test/java/org/cristalise/kernel/test/persistency/KernelSerializerContractTest.java`
- Files to update:
  - `kernel/src/main/java/org/cristalise/kernel/process/Gateway.java`
- Detailed implementation tasks:
  - Add `KernelSerializer` interface with exact signatures:
    - `String serialize(Object obj) throws InvalidDataException`
    - `Object deserialize(String data) throws InvalidDataException`
    - `<T> T deserialize(String data, Class<T> type) throws InvalidDataException`
  - Add `CastorSerializerAdapter`:
    - Accept `CastorXMLUtility` through constructor.
    - Delegate serialization/deserialization to existing Castor behavior.
    - Preserve `<NULL/>` compatibility behavior for null.
  - Add `VertxJsonSerializer`:
    - Use `io.vertx.core.json.Json.encode` and `Json.decodeValue`.
    - Implement typed decode with `Json.decodeValue(data, type)`.
    - Keep null handling explicit and deterministic.
  - In `Gateway`:
    - Add `static private KernelSerializer mSerializer;`
    - Initialize `mSerializer` in `init(...)` to `CastorSerializerAdapter` wrapping current `CastorXMLUtility`.
    - Add `static public KernelSerializer getSerializer()`.
    - Keep `getMarshaller()` unchanged for backward compatibility.
- Test updates and new tests:
  - New `KernelSerializerContractTest`:
    - Verifies roundtrip for representative object(s) through `CastorSerializerAdapter`.
    - Verifies null behavior contract.
    - Verifies typed deserialize path.
  - Ensure existing tests still compile and pass with no behavior change.
- Acceptance criteria:
  - No runtime behavior change in existing kernel flows.
  - `Gateway.getSerializer()` available and initialized.
  - Full kernel test suite remains green.

### Step 2: JSON storage implementation + storage config flag

- Goal:
  - Add JSON storage implementation and property-driven storage format selection.
- Files to create:
  - `kernel/src/main/java/org/cristalise/storage/JsonClusterStorage.java`
  - `kernel/src/test/java/org/cristalise/kernel/test/persistency/JsonClusterStorageTest.java`
  - `kernel/src/test/data/jsonstorage/` (test fixture tree)
- Files to update:
  - `kernel/src/main/java/org/cristalise/kernel/persistency/ClusterStorageManager.java`
  - `kernel/src/main/java/org/cristalise/kernel/SystemProperties.java` (or actual property holder)
  - `kernel/src/test/java/org/cristalise/kernel/test/persistency/XMLClusterStorageTest.java`
- Detailed implementation tasks:
  - Implement `JsonClusterStorage` mirroring `XMLClusterStorage` structure:
    - Default extension `.json`.
    - Read path: try `.json` then fallback `.xml`.
    - Write path: use serializer abstraction (`Gateway.getSerializer()`).
    - Keep handling of `OUTCOME` cluster aligned with current behavior.
  - Add property `kernel.storage.format`:
    - Valid values: `xml`, `json`.
    - Default remains `xml`.
  - Update `ClusterStorageManager` to instantiate storage class by `kernel.storage.format`.
- Test updates and new tests:
  - New `JsonClusterStorageTest`:
    - Put/get roundtrip for non-outcome object.
    - Put/get roundtrip for outcome data.
    - `getClusterContents` and delete behaviors.
  - Extend `XMLClusterStorageTest` with mixed read assertions:
    - XML-only data still read correctly.
    - JSON-preferred read works when both exist.
  - Add fixtures under `kernel/src/test/data/jsonstorage/...`.
- Acceptance criteria:
  - XML remains default and unchanged.
  - JSON storage mode works and passes tests.
  - Dual read fallback verified by tests.

### Step 3: Migrate caches/utilities to serializer abstraction

- Goal:
  - Remove direct marshaller calls from cache/utility classes.
- Files to update:
  - `kernel/src/main/java/org/cristalise/kernel/utils/ActDefCache.java`
  - `kernel/src/main/java/org/cristalise/kernel/utils/AgentDescCache.java`
  - `kernel/src/main/java/org/cristalise/kernel/utils/DescriptionObject.java`
  - `kernel/src/main/java/org/cristalise/kernel/utils/DescriptionObjectCache.java`
  - `kernel/src/main/java/org/cristalise/kernel/utils/DomainContextCache.java`
  - `kernel/src/main/java/org/cristalise/kernel/utils/ItemDescCache.java`
  - `kernel/src/main/java/org/cristalise/kernel/utils/PropertyDescriptionCache.java`
  - `kernel/src/main/java/org/cristalise/kernel/utils/RoleDescCache.java`
  - `kernel/src/main/java/org/cristalise/kernel/utils/StateMachineCache.java`
- Detailed implementation tasks:
  - Replace `Gateway.getMarshaller().marshall/unmarshall` with `Gateway.getSerializer().serialize/deserialize`.
  - Keep object types and exception semantics unchanged.
  - Rename local vars from `xml` naming to neutral names where touched.
- Test updates and new tests:
  - Update related cache tests (existing) to compile and run with serializer abstraction.
  - Add focused unit tests for at least two cache classes to assert identical behavior before/after migration.
- Acceptance criteria:
  - No migrated class directly calls `Gateway.getMarshaller()`.
  - Cache behavior remains unchanged and tests pass.

### Step 4: Migrate proxy/entity runtime paths to serializer abstraction

- Goal:
  - Migrate core runtime proxy/entity marshalling paths.
- Files to update:
  - `kernel/src/main/java/org/cristalise/kernel/entity/proxy/AgentProxy.java`
  - `kernel/src/main/java/org/cristalise/kernel/entity/proxy/ItemProxy.java`
  - `kernel/src/main/java/org/cristalise/kernel/entity/Job.java`
  - `kernel/src/main/java/org/cristalise/kernel/entity/TraceableEntity.java`
- Detailed implementation tasks:
  - Replace direct marshaller access with serializer abstraction.
  - Preserve all wire-level payload contracts currently expected by callers in this phase.
  - Ensure error-outcome serialization semantics remain intact.
- Test updates and new tests:
  - Update existing tests covering proxy and job execution to pass with abstraction.
  - Add/extend integration tests for:
    - Agent executing job with success outcome.
    - Error job path where error info is serialized.
- Acceptance criteria:
  - Proxy/entity tests pass with no behavioral regression.
  - Runtime path still works under default XML serializer adapter.

### Step 5: Migrate predefined steps, module/resource import paths

- Goal:
  - Migrate predefined step marshalling and resource/module import paths to serializer abstraction.
- Files to update:
  - `kernel/src/main/java/org/cristalise/kernel/lifecycle/instance/predefined/*.java` (all marshalling users)
  - `kernel/src/main/java/org/cristalise/kernel/lifecycle/instance/predefined/server/*.java`
  - `kernel/src/main/java/org/cristalise/kernel/process/resource/DefaultResourceImportHandler.java`
  - `kernel/src/main/java/org/cristalise/kernel/process/module/ModuleManager.java`
  - `kernel/src/main/java/org/cristalise/kernel/process/StandardClient.java`
- Detailed implementation tasks:
  - Replace marshaller calls with serializer abstraction.
  - Keep interface contracts of predefined steps untouched.
  - Maintain bootstrap/module import behavior in XML mode.
- Test updates and new tests:
  - Update existing predefined-step tests impacted by method-level changes.
  - Add targeted tests for import handlers:
    - successful import/unmarshal path.
    - invalid payload error path.
- Acceptance criteria:
  - Bootstrap/import related tests pass in default mode.
  - No direct marshaller calls remain in touched predefined/import classes.

### Step 6: Introduce JSON outcome model (dual mode)

- Goal:
  - Add JSON-capable outcome model while preserving existing XML outcome behavior.
- Files to create:
  - `kernel/src/main/java/org/cristalise/kernel/persistency/outcome/JsonOutcome.java`
  - `kernel/src/test/java/org/cristalise/kernel/test/persistency/JsonOutcomeTest.java`
  - `kernel/src/test/java/org/cristalise/kernel/test/persistency/JsonOutcomeValidationTest.java`
- Files to update:
  - `kernel/src/main/java/org/cristalise/kernel/persistency/outcome/Outcome.java`
  - `kernel/src/main/java/org/cristalise/kernel/persistency/outcome/OutcomeInitiator.java`
  - `kernel/src/main/java/org/cristalise/kernel/persistency/outcome/QueryOutcomeInitiator.java`
  - `kernel/src/main/java/org/cristalise/kernel/persistency/outcome/OutcomeValidator.java`
  - `kernel/src/main/java/org/cristalise/kernel/persistency/outcome/SchemaValidator.java`
- Detailed implementation tasks:
  - Implement `JsonOutcome` with path metadata parity required by storage and callers.
  - Add boundary methods/utilities so call sites can operate without assuming XML-only outcome.
  - Keep existing XML DOM/XPath behavior untouched in this PR.
  - Add extension point for JSON validation (minimal initial implementation acceptable).
- Test updates and new tests:
  - `JsonOutcomeTest`:
    - Metadata extraction from path.
    - Payload read/write basics.
  - `JsonOutcomeValidationTest`:
    - Valid JSON payload accepted.
    - Invalid JSON payload rejected with expected exception type.
  - Ensure existing outcome tests remain green.
- Acceptance criteria:
  - Both XML `Outcome` and `JsonOutcome` coexist and are test-covered.
  - No regression in existing XML outcome tests.

### Step 7: Boot resources and mapfile transition support

- Goal:
  - Introduce mixed-mode boot resource loading and reduce hard dependency on Castor mapfiles for startup path.
- Files to update:
  - `kernel/src/main/java/org/cristalise/kernel/process/Gateway.java`
  - `kernel/src/main/java/org/cristalise/kernel/process/resource/Resource.java`
  - `kernel/src/main/java/org/cristalise/kernel/process/resource/DefaultResourceImportHandler.java`
  - Boot resource loading paths under `kernel/src/main/resources/boot/**` (add JSON in staged way)
  - `kernel/src/main/resources/mapFiles/**` (deprecation notes/index handling as needed)
- Detailed implementation tasks:
  - Add boot loader logic to accept JSON resources where available, fallback to XML.
  - Keep mapFiles available for compatibility; prevent startup from hard-failing if JSON path is selected.
  - Document transition constraints in code comments where non-obvious.
- Test updates and new tests:
  - Add bootstrap tests for mixed-mode resources:
    - JSON resource preferred when present.
    - XML fallback when JSON absent.
  - Ensure existing bootstrap tests still pass.
- Acceptance criteria:
  - Kernel boot succeeds with mixed boot resource set.
  - XML-only environments continue working.

### Step 8: Default JSON write path and dependency demotion

- Goal:
  - Switch default runtime mode toward JSON path and demote Castor to compatibility profile where possible.
- Files to update:
  - `kernel/src/main/java/org/cristalise/kernel/process/Gateway.java`
  - `kernel/src/main/java/org/cristalise/kernel/persistency/ClusterStorageManager.java`
  - `kernel/pom.xml`
  - Any config defaults in kernel resources/properties files used by tests.
- Detailed implementation tasks:
  - Set default serializer/storage selection to JSON mode.
  - Keep explicit compatibility override to XML mode for rollback.
  - Update `pom.xml`:
    - Move Castor dependencies out of default runtime path if still needed only for compatibility.
    - Keep compatibility profile to run legacy import/read tests.
- Test updates and new tests:
  - Update test setup to run full suite in JSON default mode.
  - Add a focused compatibility test run path (XML compatibility profile) for legacy read.
- Acceptance criteria:
  - Full kernel suite green in JSON default mode.
  - Legacy XML read path explicitly verified.

### Step 9 (optional hardening): Remove Castor runtime path from kernel default build

- Goal:
  - Remove remaining default runtime Castor usage and dead compatibility branches where approved.
- Files to update:
  - All remaining files with `Gateway.getMarshaller()` usage.
  - `kernel/src/main/java/org/cristalise/kernel/utils/CastorXMLUtility.java` (retain only if in compatibility profile).
  - `kernel/pom.xml`
- Detailed implementation tasks:
  - Replace final `getMarshaller()` usages or isolate behind compatibility profile guards.
  - Remove dead code paths and stale imports.
  - Optionally deprecate and/or remove `Gateway.getMarshaller()` from default path.
- Test updates and new tests:
  - Add static verification step in tests/build checks to fail if disallowed Castor imports/usages remain in default source set.
  - Re-run full kernel tests in default profile and compatibility profile.
- Acceptance criteria:
  - No Castor runtime dependency in default kernel build path.
  - Compatibility profile remains functional if retained.

## Issue execution and PR policy

- Exactly one PR per issue.
- PR merge target is `develop`.
- Each PR must include:
  - code changes,
  - test updates,
  - new tests defined in the issue,
  - short migration notes in PR description.
- PR ordering:
  - Merge in numeric order from Issue 1 to Issue 8.
  - Issue 9 is optional hardening and can be merged last.
