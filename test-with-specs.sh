#!/bin/sh

echo 'Disabling :lang on startrek.core\n'
perl -pi -e 's/:lang :core.typed/;:lang :core.typed/g' src/startrek/core.clj
lein test :only startrek.test-with-specs-startrek
perl -pi -e 's/;:lang :core.typed/:lang :core.typed/g' src/startrek/core.clj
