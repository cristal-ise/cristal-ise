export enum LogLevel {OFF=0, ERROR=1, WARNING=2, INFO=3, DEBUG=4, TRACE=5};

export class Logger {

  private prefix = ""
  private configLevel = LogLevel.DEBUG;

  constructor(pckg: string, clazz: string) {
    this.prefix = pckg + "." + clazz;
  }

  public error(...args: any[]): void {
    this.log(LogLevel.ERROR, args)
  }

  public warn(...args: any[]): void {
    this.log(LogLevel.WARNING, args)
  }

  public info(...args: any[]): void {
    this.log(LogLevel.INFO, args)
  }

  public debug(...args: any[]): void {
    this.log(LogLevel.DEBUG, args)
  }

  public trace(...args: any[]): void {
    this.log(LogLevel.TRACE, args)
  }

  private log(msgLevel: LogLevel, args: any[]): void {
    if (this.configLevel < msgLevel) {
      return
    }

    let prefixToUse = this.prefix

    // Extend prefix with method name if it was provided in the first paramater
    if (typeof args[0] === 'string' && args[0].toString().endsWith('()')) {
      const method: string = args.shift();
      prefixToUse += '.' + method;
    }

    if (msgLevel === LogLevel.ERROR) {
      console.error(prefixToUse, args);
    }
    else if (msgLevel === LogLevel.WARNING) {
      console.warn(prefixToUse, args);
    }
    else if (msgLevel === LogLevel.INFO) {
      console.log(prefixToUse, ...args);
    }
    else if (msgLevel === LogLevel.DEBUG) {
      // eslint-disable-next-line no-restricted-syntax
      console.debug(prefixToUse, ...args);
    }
    else if (msgLevel === LogLevel.TRACE) {
      // eslint-disable-next-line no-restricted-syntax
      console.trace(prefixToUse, ...args);
    }
  }
}
