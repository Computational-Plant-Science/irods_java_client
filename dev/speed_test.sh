#
# Get statistics on transfer speeds of pyicmd, icommands, and jvmicmd
#
# To run this script, icommands, pyicmd, and jvmicmd must be installed.
# iinit must also be run and set to the same values as that given as input
# to this script
#
# icd must also be set to the same as the $path sent to this script

host="${1}"
username="${2}"
password="${3}"
path="${4}"

CSV_HEADER="Program"
CSV_JVMICMD="jvmicmd"
CSV_PYICMD="pyicmd"
CSV_ICMD="icommands"

icd $path

function timeit(){
  local start=`date +%s`
  "$@"
  local end=`date +%s`

  time="$((end-start))"
}

function copy()
{
  local size="${1}"

  CSV_HEADER="$CSV_HEADER,$size"

  echo "Copying $size ----------------------------------------"
  fallocate -l $size file_$size.test

  echo "jvmicmd"
  timeit jvmicmd $host $username -s $password -z tempZone put -f file_$size.test $path/file_$size.test
  CSV_JVMICMD="$CSV_JVMICMD,$time"
  echo "$time seconds"
  irm -f file_$size.test

  echo "pyicmd"
  timeit pyicmd put $path file_$size.test
  CSV_PYICMD="$CSV_PYICMD,$time"
  echo "$time seconds"
  irm -f file_$size.test

  echo "iCommands"
  timeit iput file_$size.test
  CSV_ICMD="$CSV_ICMD,$time"
  echo "$time seconds"
  irm -f file_$size.test
}

copy "1MB"
copy "10MB"
copy "100MB"
copy "1GB"
copy "5GB"
#copy "10GB"

echo $CSV_HEADER > results.log
echo $CSV_JVMICMD >> results.log
echo $CSV_PYICMD >> results.log
echo $CSV_ICMD >> results.log
