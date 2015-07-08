# Contributing to stagemonitor

## Contributor License Agreement (CLA)
If you want to submit a non-trivial pull request like implementing a new feature, you have to sign a CLA.
But don't worry, the process is really simple.

### Sign the CLA
To sign the CLA (which was created with [Harmony](http://selector.harmonyagreements.org)), all you have to do is to
include an additional file to your pull request under `cla/contributors/[your-github-userid].md`. The content of the
file differs if you are a individual or are contributing on behalf of an entity.

(This method of CLA signing is borrowed from [Medium's open source project](https://github.com/medium/opensource).)

### Entity CLA
If you are contributing on behalf of your employer, or as part of your role as an employee, you have to read and sign
the ([entity CLA](https://github.com/stagemonitor/stagemonitor/blob/master/cla/cla-entity-1.0.md)) in the name of your employer. You have to make sure that that's okay before you sign. Note that you also have to sign the individual CLA to cover any contributions which are not owned by your employer (for example if you contribute in your spare time).


Put the following in the file `cla/contributors/[your-github-userid].md`:

```
[date (yyyy-MM-dd)]

I hereby agree to the terms of the Entity Contributors License
Agreement, with MD5 checksum 3230b63601162567219de517a1be22b4.

I furthermore declare that I am authorized and able to make this
agreement and sign this declaration.

Signed,

[your full name], [name of employer]
https://github.com/[your-github-userid]

[date (yyyy-MM-dd)]

I hereby agree to the terms of the Individual Contributors License
Agreement, with MD5 checksum 3455f69ed223b9e5712f8bb3abf38190.

I furthermore declare that I am authorized and able to make this
agreement and sign this declaration.

Signed,

[your full name]
https://github.com/[your-github-userid]
```

You can confirm the MD5 checksum of the CLA by running the md5 program over cla-entity-1.0.md:

```
md5 cla-entity-1.0.md
MD5 (cla-entity-1.0.md) = 3230b63601162567219de517a1be22b4
```

If the output is different from above, do not sign the CLA and let us know.

### Individual CLA
If you are a individual wanting to contribute, you have to read and sign the
([individual CLA](https://github.com/stagemonitor/stagemonitor/blob/master/cla/cla-individual-1.0.md))

Put the following in the file `cla/contributors/[your-github-userid].md`:

```
[date (yyyy-MM-dd)]

I hereby agree to the terms of the Individual Contributors License
Agreement, with MD5 checksum 3455f69ed223b9e5712f8bb3abf38190.

I furthermore declare that I am authorized and able to make this
agreement and sign this declaration.

Signed,

[your full name]
https://github.com/[your-github-userid]
```

You can confirm the MD5 checksum of the CLA by running the md5 program over cla-individual-1.0.md:

```
md5 cla-individual-1.0.md
MD5 (cla-individual-1.0.md) = 3455f69ed223b9e5712f8bb3abf38190
```

If the output is different from above, do not sign the CLA and let us know.
