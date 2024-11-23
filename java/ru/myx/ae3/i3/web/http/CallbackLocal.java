package ru.myx.ae3.i3.web.http;

import ru.myx.ae3.base.BaseFunction;
import ru.myx.ae3.base.BaseMessage;
import ru.myx.ae3.base.BaseObject;
import ru.myx.ae3.exec.Exec;
import ru.myx.ae3.exec.ExecProcess;
import ru.myx.ae3.exec.ResultHandler;
import ru.myx.ae3.flow.ObjectTarget;

interface CallbackLocal extends ObjectTarget<BaseMessage> {
	
	
	static final class FunctionQueryToCallback implements CallbackLocal {
		
		
		private final ExecProcess ctx;

		private final BaseFunction callback;

		FunctionQueryToCallback(final ExecProcess ctx, final BaseFunction callback) {
			
			this.ctx = Exec.createProcess(HttpServerParser.CTX, ctx, "HttpServer.QueryParser-2-BaseCallback Context");
			this.callback = callback;
		}

		@Override
		public boolean absorb(final BaseMessage x) {
			
			
			if (x == null) {
				Exec.callAsyncForkUnrelated(//
						this.ctx,
						"HttpServer.onRequest: null query",
						this.callback,
						BaseObject.UNDEFINED,
						ResultHandler.FU_BNN_NXT,
						BaseObject.UNDEFINED);
				return true;
			}
			
			Exec.callAsyncForkUnrelated(//
					this.ctx,
					"HttpServer.onRequest: query: " + x,
					this.callback,
					BaseObject.UNDEFINED,
					ResultHandler.FU_BNN_NXT,
					x);
			return true;
		}

		@Override
		public String toString() {
			
			
			return this.getClass().getSimpleName() + "(" + this.callback + ")";
		}

		@Override
		public Class<? extends BaseMessage> accepts() {
			
			
			return BaseMessage.class;
		}

		@Override
		public void close() {
			
			
			// ignore
		}
	}

}
