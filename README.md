# SpacemiT Device Owner (appliance DPC)

Makes the device **fully managed automatically on first boot** — no QR code, NFC
or adb. For a device you build and own and want to behave as an appliance
(kiosk, dedicated fleet). This is "Option 3": a baked‑in DPC that enrolls itself.

## How it works

On first boot `AutoEnrollReceiver` (direct‑boot aware) starts `EnrollService`,
which:
1. checks it is not already the owner and `checkProvisioningPrecondition(
   ACTION_PROVISION_MANAGED_DEVICE)` returns `STATUS_OK`;
2. calls `DevicePolicyManager.provisionFullyManagedDevice(...)` with
   `SpacemitDeviceAdminReceiver` as the admin (the supported `@SystemApi`, no
   hidden `setDeviceOwner`);
3. sets `DEVICE_PROVISIONED` / `USER_SETUP_COMPLETE` so the device leaves setup;
4. applies the baked policy (`applyBakedPolicy` — currently `DISALLOW_ADD_USER`
   and `DISALLOW_FACTORY_RESET`; extend with lock‑task/kiosk or SpacemiT policy
   keys via the policy SDK).

Platform‑signed + privileged, so it holds `MANAGE_PROFILE_AND_DEVICE_OWNERS`
(`signature|role`) and `WRITE_SECURE_SETTINGS` by signature.

## The one hard requirement — first‑boot ordering

Device owner can be set **only** while the device is factory‑fresh: no device
owner, **setup not yet complete**, no accounts. If a SetupWizard marks the
device provisioned before this app runs, `checkProvisioningPrecondition` returns
`STATUS_USER_SETUP_COMPLETED (4)` and enrollment is skipped.

So the appliance product must **not let `com.android.provision` (or any
SetupWizard) complete setup first**. Either exclude it from the product, or rely
on this app's `LOCKED_BOOT_COMPLETED` (direct‑boot) receiver running earlier.
This app marks setup complete itself after enrolling.

## Enable it

Opt‑in, off by default:

```make
SPACEMIT_AUTO_DPC := true
```

(see `device/spacemit/common/spacemit-features.mk`). Installs to
`/system/priv-app/SpacemitDpc`.

## Verify (userdebug)

```
adb shell dumpsys device_policy | grep -i "Device Owner"
adb shell dpm list-owners
```
To re‑test from scratch: `adb shell cmd device_policy clear-device-owner com.spacemit.dpc`
or factory‑reset, then reboot.

## Relationship to the rest

This is the **device owner** itself. `SpacemitPolicyService` / `SpacemitOemConfig`
are for applying SpacemiT‑specific settings; a baked appliance can call the
policy SDK from `applyBakedPolicy`, or an EMM can take over as owner instead of
this app (don't ship both as owner).
